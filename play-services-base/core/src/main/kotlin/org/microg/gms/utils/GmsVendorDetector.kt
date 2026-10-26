/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.io.File
import java.util.zip.ZipFile

/**
 * Works out which GMS implementation ("vendor") a ReVanced/Morphe-patched app was built for.
 *
 * A patched app is permanently bound to one GMS package: the patch rewrites the
 * `com.google.android.gms` package references of the original app to the vendor package name that
 * was configured at patch time (`GmsCoreVendorGroupId`). The Android platform resolves that package
 * at runtime, so an app patched for `app.revanced.android.gms` cannot talk to
 * `org.microg.android.gms`, no matter how many different builds are installed side by side.
 *
 * The only reliable, permission-free way to read that binding back is to look for the dotted vendor
 * package names in the patched DEX files. Installed APKs are world readable, the scan is cached per
 * APK size/mtime, and only apps whose package name carries a known patcher marker are ever scanned,
 * so the cost stays at a handful of small reads.
 */
object GmsVendorDetector {

    private const val TAG = "MicroG-VendorCompat"
    private const val PREFS = "microg_vendor_scan"

    /** Upper bound for the number of patched apps that get scanned in one pass. */
    private const val MAX_APPS = 12

    /** Byte budget for one APK and for a whole pass, to keep Self-Check responsive. */
    private const val MAX_BYTES_PER_APK = 48L * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 96L * 1024 * 1024

    private const val CHUNK = 1 shl 20
    private const val EARLY_EXIT_HITS = 12
    private const val CONFIDENT_HITS = 3

    private const val GOOGLE = "com.google.android.gms"

    /**
     * Every GMS implementation this build knows by name. Order is used for reporting, so keep the
     * microG-compatible ones first and stock Google Play services last.
     */
    val KNOWN_VENDORS: List<String> = listOf(
        "app.revanced.android.gms",
        "app.morphe.android.gms",
        "org.microg.android.gms",
        "org.microg.gms",
        "app.rvx.android.gms",
        "com.mgoogle.android.gms",
        GOOGLE
    )

    private val NEEDLE_BYTES: Map<String, ByteArray> =
        (KNOWN_VENDORS + GOOGLE).distinct().associateWith { it.toByteArray(Charsets.UTF_8) }

    private val MAX_NEEDLE: Int = NEEDLE_BYTES.values.maxOfOrNull { it.size } ?: 24

    data class Implementation(
        val packageName: String,
        val versionName: String?,
        val isThisBuild: Boolean
    )

    data class PatchedApp(
        val packageName: String,
        val label: String,
        val patcherLabelRes: Int?,
        /** Best guess of the GMS package this app was patched for, `null` when unknown. */
        val expectedVendor: String?,
        /** Raw occurrences per vendor package found in the app DEX files. */
        val hits: Map<String, Int>,
        /** `true` when the guess is backed by enough hits to be trusted. */
        val confident: Boolean
    )

    /** GMS implementations that are currently installed, keyed by package name. */
    fun installedImplementations(context: Context): List<Implementation> {
        val pm = context.packageManager
        val own = context.packageName
        return KNOWN_VENDORS.mapNotNull { name ->
            try {
                val info = pm.getPackageInfo(name, 0)
                Implementation(name, info.versionName, name == own)
            } catch (e: PackageManager.NameNotFoundException) {
                null
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to query $name", t)
                null
            }
        }
    }

    fun isInstalled(context: Context, vendor: String?): Boolean {
        if (vendor.isNullOrEmpty()) return false
        return try {
            context.packageManager.getPackageInfo(vendor, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to query $vendor", t)
            false
        }
    }

    /**
     * Scans every installed app that carries a known patcher marker and reports the GMS package it
     * is bound to. Results are cached per APK size/mtime in private preferences.
     */
    fun patchedApps(context: Context): List<PatchedApp> {
        val pm = context.packageManager
        val own = context.packageName
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val result = ArrayList<PatchedApp>()
        var remainingBudget = MAX_TOTAL_BYTES

        val packages = try {
            pm.getInstalledPackages(0)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to enumerate packages", t)
            return result
        }

        for (pkg in packages) {
            if (result.size >= MAX_APPS) break
            val name = pkg.packageName ?: continue
            if (name == own) continue
            val patcherLabelRes = AppPatcherDetector.getUsingPackageName(name) ?: continue
            val appInfo = pkg.applicationInfo ?: continue
            val label = try {
                appInfo.loadLabel(pm).toString()
            } catch (t: Throwable) {
                name
            }

            val source = appInfo.sourceDir
            val apk = if (source != null) File(source) else null
            if (apk == null || !apk.isFile) {
                result.add(PatchedApp(name, label, patcherLabelRes, null, emptyMap(), false))
                continue
            }

            val stamp = "${apk.length()}:${apk.lastModified()}"
            val cached = prefs.getString(name, null)
            var vendor: String? = null
            var confident = false
            var hits: Map<String, Int> = emptyMap()

            if (cached != null && cached.startsWith("$stamp|")) {
                val parts = cached.split('|')
                vendor = parts.getOrNull(1)?.takeIf { it != "-" && it.isNotEmpty() }
                confident = parts.getOrNull(2) == "1"
            } else if (remainingBudget > 0) {
                remainingBudget -= minOf(remainingBudget, apk.length().coerceAtLeast(1L))
                hits = scanApk(apk)
                vendor = pickVendor(hits)
                confident = vendor != null && (hits[vendor] ?: 0) >= CONFIDENT_HITS
                prefs.edit()
                    .putString(name, "$stamp|${vendor ?: "-"}|${if (confident) 1 else 0}")
                    .apply()
            }

            result.add(PatchedApp(name, label, patcherLabelRes, vendor, hits, confident))
        }

        return result
    }

    private fun scanApk(apk: File): Map<String, Int> {
        val counts = HashMap<String, Int>()
        var budget = MAX_BYTES_PER_APK
        try {
            ZipFile(apk).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements() && budget > 0) {
                    val entry = entries.nextElement()
                    if (!entry.name.endsWith(".dex", ignoreCase = true)) continue
                    zip.getInputStream(entry).use { stream ->
                        val buffer = ByteArray(CHUNK + MAX_NEEDLE)
                        var carry = 0
                        while (budget > 0) {
                            val read = stream.read(buffer, carry, CHUNK)
                            if (read <= 0) break
                            budget -= read
                            val length = carry + read
                            countMatches(buffer, length, counts)
                            if (hasEarlyExit(counts)) return counts
                            carry = minOf(MAX_NEEDLE - 1, length)
                            if (carry > 0) System.arraycopy(buffer, length - carry, buffer, 0, carry)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Unable to scan $apk", t)
        }
        return counts
    }

    private fun countMatches(buffer: ByteArray, length: Int, counts: MutableMap<String, Int>) {
        // DEX strings are MUTF-8 and not aligned, so a plain byte search is the correct approach.
        for ((vendor, needle) in NEEDLE_BYTES) {
            val found = countOccurrences(buffer, length, needle)
            if (found > 0) counts[vendor] = (counts[vendor] ?: 0) + found
        }
    }

    private fun countOccurrences(haystack: ByteArray, length: Int, needle: ByteArray): Int {
        if (length < needle.size) return 0
        var count = 0
        val last = length - needle.size
        var i = 0
        while (i <= last) {
            var j = 0
            while (j < needle.size && haystack[i + j] == needle[j]) j++
            if (j == needle.size) {
                count++
                i += needle.size
            } else {
                i++
            }
        }
        return count
    }

    private fun hasEarlyExit(counts: Map<String, Int>): Boolean =
        counts.any { (vendor, hits) -> vendor != GOOGLE && hits >= EARLY_EXIT_HITS }

    /**
     * Prefers a microG-style vendor as soon as it is backed by a few hits: patched apps keep some
     * `com.google.android.gms` string literals (permission names, SDK suffixes), so a plain
     * maximum would misreport most patched apps as stock-Google apps.
     */
    private fun pickVendor(hits: Map<String, Int>): String? {
        val google = hits[GOOGLE] ?: 0
        val microG = hits.filterKeys { it != GOOGLE }.maxByOrNull { it.value }
        if (microG != null && microG.value >= CONFIDENT_HITS && microG.value * 3 >= google) {
            return microG.key
        }
        if (google > 0) return GOOGLE
        if (microG != null && microG.value >= CONFIDENT_HITS) return microG.key
        return null
    }
}
