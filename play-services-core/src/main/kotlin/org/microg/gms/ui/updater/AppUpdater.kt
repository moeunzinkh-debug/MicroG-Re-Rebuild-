/*
 * Copyright (C) 2026 Morphe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.microg.gms.ui.updater

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.text.method.LinkMovementMethod
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import com.google.android.gms.BuildConfig
import com.google.android.gms.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.microg.gms.ui.MainSettingsActivity
import org.microg.gms.ui.updater.AppUpdater.UPDATE_COOLDOWN_MS
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipFile

/**
 * In-app updater for MicroG-RE.
 *
 * Checks GitHub for the latest release, prompts the user on launch (or via a notification
 * when running in the background), and opens the release download in the system browser so
 * REQUEST_INSTALL_PACKAGES is not required.
 */
object AppUpdater {

    /** Intent extra used by the update notification to force the update prompt. */
    const val EXTRA_CHECK_UPDATE = "extra_check_update"

    private const val PRIMARY_RELEASES_URL = "https://api.github.com/repos/moeunzinkh-debug/MicroG-Re-Rebuild-/releases"
    private const val PRIMARY_WEB_RELEASES_URL = "https://github.com/moeunzinkh-debug/MicroG-Re-Rebuild-/releases"
    private const val RELEASES_URL = "https://api.github.com/repos/MorpheApp/MicroG-RE/releases"
    private const val UPDATE_URL = "$RELEASES_URL/latest"
    private const val WEB_RELEASES_URL = "https://github.com/MorpheApp/MicroG-RE/releases"
    private val isOriginalMicrog: Boolean
        get() = BuildConfig.BASE_PACKAGE_NAME == "com.google"
    private const val PREFS_NAME = "org.microg.gms_updater"
    private const val PREFS_IGNORED_VERSION = "ignored_version"
    private const val PREFS_LAST_CHECK = "last_check"
    private const val PREFS_NOTIF_ASKED = "notif_permission_asked"

    private var isAbiWarningShowing = false
    private var isUpdateDialogShowing = false
    private var isDownloadInProgress = false

    /** Settings switch key (default SharedPreferences): include pre-release / dev builds. */
    const val PREFS_INCLUDE_PRERELEASE = "pref_include_prerelease"
    private const val NOTIFICATION_CHANNEL = "updates"
    private const val NOTIFICATION_ID = 0x5555
    private const val REQUEST_NOTIFICATION_PERMISSION = 0x5556
    private const val MAX_NOTES_LENGTH = 8000
    private const val USER_AGENT = "MicroG-RE-Updater"
    private const val TIMEOUT_MS = 15000
    private val TAG: String
        get() = if (isOriginalMicrog) "MicroG-Orig:Updater" else "MicroG-RE:Updater"

    /** Automatic checks run at most once every [UPDATE_COOLDOWN_MS]; manual checks always run. */
    private const val UPDATE_COOLDOWN_MS = 24 * 60 * 60 * 1000L

    class UpdateInfo {
        var tagName: String = ""
        var version: String = ""
        var releaseNotes: String? = null
        var assetName: String = ""
        var assetUrl: String = ""
        var assetSize: Long = 0
        var prerelease: Boolean = false
    }

    // -----------------------------------------------------------------------------------
    // Public API (Java-friendly)
    // -----------------------------------------------------------------------------------

    /** Called when the settings activity launches. Checks for updates and prompts if needed. */
    @JvmStatic
    fun checkOnLaunch(activity: Activity) {
        if (maybeWarnWrongAbiVariant(activity)) return
        val intent = activity.intent
        val force = intent?.getBooleanExtra(EXTRA_CHECK_UPDATE, false) == true
        intent?.removeExtra(EXTRA_CHECK_UPDATE)
        if (force) {
            check(activity, force = true)
            return
        }
        if (System.currentTimeMillis() - prefs(activity).getLong(PREFS_LAST_CHECK, 0) < UPDATE_COOLDOWN_MS) {
            return
        }
        check(activity, force = false)
    }

    /** Manual "Check for updates" action: always runs and reports the result. */
    @JvmStatic
    fun checkManually(activity: Activity) {
        check(activity, force = true, manual = true)
    }

    /** Opens [update] download URL in the system browser (no REQUEST_INSTALL_PACKAGES needed). */
    @JvmStatic
    fun runUpdateFlow(activity: Activity, update: UpdateInfo?) {
        val fallback = if (isOriginalMicrog) PRIMARY_WEB_RELEASES_URL else WEB_RELEASES_URL
        val url = update?.assetUrl?.takeIf { it.isNotEmpty() } ?: fallback
        ensureUpdateNotificationPermission(activity)
        openDownloadInBrowser(activity, url)
    }

    /** Resolves the 'noicon' build variant URL and opens it in the system browser. */
    @JvmStatic
    fun downloadAndInstallNoIconVariant(activity: Activity) {
        ensureUpdateNotificationPermission(activity)

        isDownloadInProgress = true
        var isCancelled = false

        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            val padding = (16 * activity.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.hide_launcher_icon_updating_title)
            .setView(progress)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                isCancelled = true
                isDownloadInProgress = false
            }
            .setOnCancelListener {
                isCancelled = true
                isDownloadInProgress = false
            }
            .create()
        dialog.show()

        Thread {
            val update = fetchLatestUpdate(
                noiconVariant = true,
                includePrerelease = includePrerelease(activity),
                strictVariantMatch = true
            )
            if (isCancelled) {
                isDownloadInProgress = false
                return@Thread
            }

            val cleanVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
                .substringBefore(" ").substringBefore("(")
            val isArm64 = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
            val assetFileName = if (isArm64) "microg-$cleanVersion-noicon-arm64-v8a.apk" else "microg-$cleanVersion-noicon.apk"
            val fallbackUrl = "https://github.com/MorpheApp/MicroG-RE/releases/download/$cleanVersion/$assetFileName"

            val resolvedUrl = if (update?.assetUrl?.isNotEmpty() == true && update.assetName.lowercase().contains("noicon")) {
                update.assetUrl
            } else {
                fallbackUrl
            }

            runOnUiThread(activity) {
                dialog.dismiss()
                isDownloadInProgress = false
                if (!isCancelled) {
                    openDownloadInBrowser(activity, resolvedUrl)
                }
            }
        }.start()
    }

    /** Posts a notification that an update is available (used by the background worker). */
    @JvmStatic
    fun postUpdateNotification(context: Context, update: UpdateInfo) {
        // On Android 13+ a background receiver cannot prompt for POST_NOTIFICATIONS; if it
        // is missing the notification would be silently dropped, so skip posting entirely.
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        ensureNotificationChannel(context)
        val pending = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainSettingsActivity::class.java)
                .putExtra(EXTRA_CHECK_UPDATE, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(context, NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(context.getString(R.string.update_notification_text, update.version))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        context.getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, builder.build())
    }

    /** Dismisses the "update available" notification (e.g. when the user ignores the update). */
    @JvmStatic
    fun dismissUpdateNotification(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    /** True when [update] is newer than the installed version. */
    @JvmStatic
    fun isNewerThanInstalled(update: UpdateInfo): Boolean {
        return compareVersions(update.version, BuildConfig.VERSION_NAME) > 0
    }

    // -----------------------------------------------------------------------------------
    // Core logic
    // -----------------------------------------------------------------------------------

    private fun check(activity: Activity, force: Boolean, manual: Boolean = false) {
        if (!force) {
            prefs(activity).edit().putLong(PREFS_LAST_CHECK, System.currentTimeMillis()).apply()
        }
        Thread {
            val update = fetchLatestUpdate(
                activity.resources.getBoolean(R.bool.hide_launcher_icon_available),
                includePrerelease(activity)
            )
            runOnUiThread(activity) {
                if (update == null) {
                    if (manual) toast(activity, R.string.update_check_failed)
                    return@runOnUiThread
                }
                if (!isNewerThanInstalled(update)) {
                    if (manual) showUpToDateDialog(activity, update)
                    return@runOnUiThread
                }
                if (!force) {
                    val ignored = prefs(activity).getString(PREFS_IGNORED_VERSION, null)
                    if (ignored != null && compareVersions(update.version, ignored) <= 0) {
                        return@runOnUiThread
                    }
                }
                showUpdateDialog(activity, update)
            }
        }.start()
    }

    /**
     * Keeps the action buttons inside the dialog.
     *
     * The dialog is a floating window with a capped height, and whatever does not fit is clipped
     * at the bottom - exactly where the buttons sit. Everything else in the layout has a fixed
     * height, so the release notes are the one part that has to give: give them whatever room is
     * left over once the rest is laid out and let them scroll inside it. Without this a long
     * changelog silently hides the last action.
     */
    private fun capReleaseNotes(
        activity: Activity,
        root: android.view.View,
        scroll: android.widget.ScrollView,
        notes: android.widget.TextView,
    ) {
        val available = root.height
        if (available <= 0 || root.width <= 0) return
        val density = activity.resources.displayMetrics.density
        val wanted = notes.height

        // Measure the dialog again without the notes to learn how much room the rest of it needs.
        scroll.layoutParams = scroll.layoutParams.apply { height = 0 }
        root.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(root.width, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
        )
        val rest = root.measuredHeight
        val allowed = (available - rest)
            .coerceIn((48 * density).toInt(), (120 * density).toInt())
        scroll.layoutParams = scroll.layoutParams.apply { height = minOf(wanted, allowed) }
        scroll.requestLayout()
    }

    private fun showUpdateDialog(activity: Activity, update: UpdateInfo) {
        if (isUpdateDialogShowing || isDownloadInProgress || isAbiWarningShowing) return
        isUpdateDialogShowing = true
        val view = activity.layoutInflater.inflate(R.layout.dialog_update, null)
        var current = update
        val titleView = view.findViewById<android.widget.TextView>(R.id.update_title)
        val messageView = view.findViewById<android.widget.TextView>(R.id.update_message)
        val notesView = view.findViewById<android.widget.TextView>(R.id.update_notes)
        val notesHeader = view.findViewById<android.widget.TextView>(R.id.update_notes_header)
        val notesScroll = view.findViewById<android.widget.ScrollView>(R.id.update_notes_scroll)

        fun refreshDialog(u: UpdateInfo) {
            titleView.setText(
                if (u.prerelease) R.string.update_available_prerelease_title
                else R.string.update_available_title
            )
            messageView.text =
                activity.getString(R.string.update_available_message, u.version, BuildConfig.VERSION_NAME)
            val notes = u.releaseNotes?.let { renderReleaseNotes(it) }
            if (!notes.isNullOrBlank()) {
                notesView.text = HtmlCompat.fromHtml(notes, HtmlCompat.FROM_HTML_MODE_COMPACT)
                notesView.movementMethod = LinkMovementMethod.getInstance()
                notesView.visibility = android.view.View.VISIBLE
                notesHeader.visibility = android.view.View.VISIBLE
                notesScroll.visibility = android.view.View.VISIBLE
                // Cap the notes area after layout so long changelogs scroll instead of pushing
                // the action buttons off the bottom of the dialog.
                notesScroll.layoutParams.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                notesScroll.post { capReleaseNotes(activity, view, notesScroll, notesView) }
            } else {
                notesView.visibility = android.view.View.GONE
                notesHeader.visibility = android.view.View.GONE
                notesScroll.visibility = android.view.View.GONE
            }
        }
        refreshDialog(current)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setOnDismissListener { isUpdateDialogShowing = false }
            .create()
        // The actions are a full width vertical stack in the layout instead of the platform
        // button bar, so a translation whose labels are longer than the English ones cannot
        // clip them or leave the left half of the bar empty.
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.update_action_update)
            .setOnClickListener {
                dialog.dismiss()
                runUpdateFlow(activity, current)
            }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.update_action_ignore)
            .setOnClickListener {
                prefs(activity).edit().putString(PREFS_IGNORED_VERSION, current.version).apply()
                dismissUpdateNotification(activity)
                dialog.dismiss()
            }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.update_action_cancel)
            .setOnClickListener { dialog.dismiss() }

        // A dev build is always on the dev channel (forced by the version-name match) and
        // cannot be toggled off without a clean install, so only show the switch on stable
        // builds where opting in or out is meaningful.
        if (isDevBuild()) {
            view.findViewById<android.view.View>(R.id.update_prerelease_row)
                .visibility = android.view.View.GONE
        }
        val switch = view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.update_prerelease_switch)
        switch.isChecked = includePrerelease(activity)
        bindPrereleaseRow(view, R.id.update_prerelease_row, switch) { checked ->
            setPrereleasePref(activity, checked)
            Thread {
                val candidate = fetchLatestUpdate(
                    activity.resources.getBoolean(R.bool.hide_launcher_icon_available),
                    checked
                )
                runOnUiThread(activity) {
                    if (candidate == null || !isNewerThanInstalled(candidate)) {
                        // Nothing newer on the newly selected channel. If the user turned the
                        // toggle off, the pre-release offer no longer applies: dismiss it.
                        if (!checked) dialog.dismiss()
                        return@runOnUiThread
                    }
                    current = candidate
                    refreshDialog(current)
                }
            }.start()
        }
        dialog.show()
    }

    /**
     * Renders GitHub-style markdown release notes into HTML for the dialog via a real
     * CommonMark parser, turning headings, lists, code blocks, emphasis and links into
     * clickable [android.text.style.URLSpan]s through [HtmlCompat]. Instead of the old
     * hand-rolled regex "sanitizer" that mangled bullets and code blocks.
     */
    private fun renderReleaseNotes(raw: String): String {
        var s = raw.trim()
        if (s.length > MAX_NOTES_LENGTH) s = s.take(MAX_NOTES_LENGTH) + "\n\u2026"
        // Escape HTML first so raw markup in the notes is never interpreted.
        s = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val parser = org.commonmark.parser.Parser.builder().build()
        val renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build()
        var html = renderer.render(parser.parse(s))
        // Bare URLs (not inside markdown links) aren't auto-linked by plain CommonMark,
        // so wrap the remaining ones that aren't already inside an href attribute.
        html = html.replace(
            Regex("""(?<!["=])(https?://[^\s<>"]+)""", RegexOption.IGNORE_CASE),
            """<a href="$1">$1</a>"""
        )
        // Render inline code and code blocks in monospace (Html maps <font face> to a span).
        html = html.replace(
            Regex("""<code[^>]*>""", RegexOption.IGNORE_CASE),
            """<font face="monospace">"""
        ).replace("</code>", "</font>")
        // Android's Html only draws list bullets in the margin right next to the text with
        // no gap, so turn <li> into explicit "• " text with a line break instead.
        html = html.replace("<li>", "\u2022 ")
            .replace("</li>", "<br>")
            .replace("<ul>", "")
            .replace("</ul>", "")
            .replace("<ol>", "")
            .replace("</ol>", "")
        return html
    }

    /**
     * Turns the pre-release switch into a settings style row: pressing anywhere on the row
     * toggles the switch, so the control is not a lone switch with a small touch target.
     */
    private fun bindPrereleaseRow(
        view: android.view.View,
        rowId: Int,
        switch: com.google.android.material.materialswitch.MaterialSwitch,
        onChanged: (Boolean) -> Unit
    ) {
        view.findViewById<android.view.View>(rowId).setOnClickListener { switch.toggle() }
        switch.setOnCheckedChangeListener { _, checked -> onChanged(checked) }
    }

    private fun showUpToDateDialog(activity: Activity, update: UpdateInfo) {
        if (isUpdateDialogShowing || isDownloadInProgress || isAbiWarningShowing) return
        isUpdateDialogShowing = true
        val view = activity.layoutInflater.inflate(R.layout.dialog_up_to_date, null)
        view.findViewById<android.widget.TextView>(R.id.up_to_date_title)
            .setText(R.string.update_up_to_date_title)
        view.findViewById<android.widget.TextView>(R.id.up_to_date_message).text =
            activity.getString(R.string.update_up_to_date_message, update.version)
        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .setOnDismissListener { isUpdateDialogShowing = false }
            .create()
        // Only a stable build can opt into the dev channel; on a dev build the channel is
        // forced by the version name, so hide the switch to avoid an unchangeable toggle.
        if (isDevBuild()) {
            view.findViewById<android.view.View>(R.id.up_to_date_prerelease_row)
                .visibility = android.view.View.GONE
        }
        // Let an up-to-date stable user opt into the dev channel right here: flipping the
        // switch re-checks for pre-release builds and jumps to the update dialog if one is
        // newer. Previously the toggle only existed once an update was already offered.
        val switch =
            view.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.up_to_date_prerelease_switch)
        switch.isChecked = includePrerelease(activity)
        bindPrereleaseRow(view, R.id.up_to_date_prerelease_row, switch) { checked ->
            setPrereleasePref(activity, checked)
            if (!checked) return@bindPrereleaseRow
            Thread {
                val candidate = fetchLatestUpdate(
                    activity.resources.getBoolean(R.bool.hide_launcher_icon_available),
                    includePrerelease = true
                )
                runOnUiThread(activity) {
                    // Nothing newer found: leave the dialog as-is, it already states that
                    // the installed version is the latest. On fetch failure the same holds
                    // for the stable line, so stay silent there too.
                    if (candidate != null && isNewerThanInstalled(candidate)) {
                        dialog.dismiss()
                        showUpdateDialog(activity, candidate)
                    }
                }
            }.start()
        }
        dialog.show()
    }

    private fun showErrorDialog(context: Context, message: String) {
        MaterialAlertDialogBuilder(context)
            .setTitle(R.string.update_failed_title)
            .setMessage(message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun openDownloadInBrowser(activity: Activity, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            Log.i(TAG, "Opening download URL in browser: $url")
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Opening browser for update failed", e)
            showErrorDialog(activity, activity.getString(R.string.update_download_failed))
        }
    }

    /**
     * Fetches the newest releasable update from GitHub. Returns null on failure.
     *
     * Checks the rebuild repository's releases first (so custom-signed updates match),
     * then falls back to upstream MorpheApp/MicroG-RE releases.
     */
    @JvmStatic
    @JvmOverloads
    fun fetchLatestUpdate(
        noiconVariant: Boolean,
        includePrerelease: Boolean,
        strictVariantMatch: Boolean = false
    ): UpdateInfo? {
        fetchLatestUpdateFromRepo(PRIMARY_RELEASES_URL, "$PRIMARY_RELEASES_URL/latest", noiconVariant, includePrerelease, strictVariantMatch)
            ?.let { return it }
        if (isOriginalMicrog) return null
        return fetchLatestUpdateFromRepo(RELEASES_URL, UPDATE_URL, noiconVariant, includePrerelease, strictVariantMatch)
    }

    private fun matchesReleaseTag(rawTag: String): Boolean {
        val lower = rawTag.trim().lowercase()
        return if (isOriginalMicrog) {
            lower.startsWith("microg-original-")
        } else {
            !lower.startsWith("microg-original-")
        }
    }

    private fun fetchLatestUpdateFromRepo(
        releasesUrl: String,
        latestUrl: String,
        noiconVariant: Boolean,
        includePrerelease: Boolean,
        strictVariantMatch: Boolean = false
    ): UpdateInfo? {
        return try {
            // Always scan the release list on multi-tag repositories so separate tags
            // (microg-re-v* vs microg-original-hw-v*) resolve independently.
            val useListScan = includePrerelease || strictVariantMatch || releasesUrl == PRIMARY_RELEASES_URL
            if (useListScan) {
                val text = httpGet("$releasesUrl?per_page=25") ?: return null
                val all = Gson().fromJson(text, JsonArray::class.java) ?: return null
                all.asSequence()
                    .filter { includePrerelease || it.asJsonObject?.get("prerelease")?.asBoolean != true }
                    .mapNotNull { el ->
                        el.asJsonObject?.let { parseReleaseObject(it, noiconVariant, strictVariantMatch) }
                    }.maxWithOrNull { a, b ->
                        compareVersions(a.version, b.version)
                    }
            } else {
                val text = httpGet(latestUrl) ?: return null
                parseReleaseObject(
                    Gson().fromJson(text, JsonObject::class.java) ?: return null,
                    noiconVariant,
                    strictVariantMatch
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun parseReleaseObject(
        root: JsonObject,
        noiconVariant: Boolean,
        strictVariantMatch: Boolean = false
    ): UpdateInfo? {
        return try {
            val tag = root.get("tag_name")?.asString ?: return null
            if (!matchesReleaseTag(tag)) return null
            val assets = root.getAsJsonArray("assets") ?: return null
            val asset = pickAsset(assets, noiconVariant, strictVariantMatch) ?: return null
            UpdateInfo().apply {
                tagName = tag
                version = normalizeVersion(tag)
                releaseNotes = root.get("body")?.asString
                assetName = asset.get("name")?.asString ?: ""
                assetUrl = asset.get("browser_download_url")?.asString ?: ""
                assetSize = asset.get("size")?.asLong ?: 0
                prerelease = root.get("prerelease")?.asBoolean ?: false
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Picks the APK matching the installed variant, so an update installs as an upgrade of
     * the same build: same ABI (arm64-v8a on 64-bit devices, universal otherwise) and the
     * same launcher-icon variant (normal vs -noicon).
     */
    private fun matchesBuildEdition(assetLowerName: String): Boolean {
        if (!assetLowerName.endsWith(".apk")) return false
        if (assetLowerName.startsWith("com.android.vending") || assetLowerName.contains("companion-vending")) {
            return false
        }
        val isHuaweiGmsAsset = assetLowerName.startsWith("com.google.android.gms") ||
            assetLowerName.contains("original-huawei") ||
            assetLowerName.contains("-hw")
        return isHuaweiGmsAsset == isOriginalMicrog
    }

    private fun pickAsset(
        assets: JsonArray,
        noiconVariant: Boolean,
        strictVariantMatch: Boolean = false
    ): JsonObject? {
        val apks = assets.mapNotNull { it.asJsonObject }.filter { obj ->
            val name = obj.get("name")?.asString?.lowercase() ?: ""
            matchesBuildEdition(name)
        }
        if (apks.isEmpty()) return null
        val arm64Preferred = Build.SUPPORTED_ABIS.any { it == "arm64-v8a" }
        fun nameOf(asset: JsonObject) = asset.get("name")?.asString?.lowercase() ?: ""
        val sameAbi = apks.filter {
            nameOf(it).contains("arm64-v8a") == arm64Preferred
        }
        val exact = sameAbi.firstOrNull { nameOf(it).contains("noicon") == noiconVariant }
        if (strictVariantMatch) {
            val anyAbiMatch = apks.firstOrNull { nameOf(it).contains("noicon") == noiconVariant }
            return exact ?: anyAbiMatch
        }
        val variantFallback = sameAbi.firstOrNull { nameOf(it).contains("noicon") != noiconVariant }
        val anyAbi = apks.firstOrNull { nameOf(it).contains("noicon") == noiconVariant }
        return exact ?: variantFallback ?: anyAbi
    }

    private fun httpGet(url: String): String? {
        return try {
            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", USER_AGENT)
            if (conn.responseCode != 200) {
                conn.disconnect()
                return null
            }
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            text
        } catch (e: Exception) {
            null
        }
    }

    // -----------------------------------------------------------------------------------
    // Version comparison
    // -----------------------------------------------------------------------------------

    private fun normalizeVersion(v: String): String {
        var s = v.trim()
        for (prefix in listOf("microg-original-hw-", "microg-original-", "microg-re-")) {
            if (s.startsWith(prefix, ignoreCase = true)) {
                s = s.substring(prefix.length)
                break
            }
        }
        s = s.removePrefix("v").removePrefix("V")
        if (s.endsWith("-hw", ignoreCase = true)) {
            s = s.dropLast(3)
        }
        return s
    }

    /** Parses a version into (core numbers, pre-release identifiers) per semver. */
    private fun semverParts(v: String): Pair<List<Int>, List<String>> {
        val main = normalizeVersion(v).substringBefore("+") // drop build metadata
        val split = main.split("-", limit = 2)
        val core = split[0].split(".").mapNotNull { it.toIntOrNull() }
        val prerelease = if (split.size > 1) split[1].split(".") else emptyList()
        return Pair(core, prerelease)
    }

    /**
     * Compares two version strings semver-style ("v7.0.0" vs "7.0.0-dev.7" vs "7.0.1").
     * A pre-release of the same base version ranks below the release. Returns <0, 0 or >0.
     */
    @JvmStatic
    fun compareVersions(a: String, b: String): Int {
        val (ca, pa) = semverParts(a)
        val (cb, pb) = semverParts(b)
        for (i in 0 until maxOf(ca.size, cb.size)) {
            val x = ca.getOrElse(i) { 0 }
            val y = cb.getOrElse(i) { 0 }
            if (x != y) return if (x > y) 1 else -1
        }
        // Same core: a release beats its own pre-releases.
        if (pa.isEmpty() && pb.isNotEmpty()) return 1
        if (pa.isNotEmpty() && pb.isEmpty()) return -1
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val x = pa.getOrElse(i) { null } ?: return -1
            val y = pb.getOrElse(i) { null } ?: return 1
            val xn = x.toIntOrNull()
            val yn = y.toIntOrNull()
            val c = when {
                xn != null && yn != null -> xn.compareTo(yn)
                xn != null -> -1 // numeric identifiers sort below alphanumeric
                yn != null -> 1
                else -> x.compareTo(y)
            }
            if (c != 0) return c
        }
        return 0
    }

    // -----------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun isDevBuild(): Boolean = BuildConfig.VERSION_NAME.contains("dev", ignoreCase = true)

    /** True when the "include pre-release builds" settings toggle is enabled OR current version is a dev build. */
    @JvmStatic
    fun includePrerelease(context: Context): Boolean =
        isDevBuild() ||
                prefsDefault(context).getBoolean(PREFS_INCLUDE_PRERELEASE, false)

    /**
     * Records that this install is on the dev channel.
     *
     * Without this, the automatic dev opt-in (version-name based) would silently vanish
     * when the user upgrades in place to a later stable release, and they would have to
     * remember to re-enable the pre-release toggle. Persisting it once keeps dev updates
     * flowing after moving to stable; turning the toggle off explicitly still opts out.
     */
    @JvmStatic
    fun persistDevChannelIfNeeded(context: Context) {
        if (!BuildConfig.VERSION_NAME.contains("dev", ignoreCase = true)) return
        val prefs = prefsDefault(context)
        if (!prefs.contains(PREFS_INCLUDE_PRERELEASE)) {
            prefs.edit().putBoolean(PREFS_INCLUDE_PRERELEASE, true).apply()
        }
    }

    private fun setPrereleasePref(context: Context, value: Boolean) {
        prefsDefault(context).edit().putBoolean(PREFS_INCLUDE_PRERELEASE, value).apply()
    }

    private fun prefsDefault(context: Context): SharedPreferences =
        androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)

    private fun runOnUiThread(activity: Activity, block: () -> Unit) {
        Handler(Looper.getMainLooper()).post { block() }
    }

    private fun toast(context: Context, resId: Int) {
        Toast.makeText(context, context.getString(resId), Toast.LENGTH_SHORT).show()
    }


    // -----------------------------------------------------------------------------------
    // ABI mismatch: arm64-only APK on a 32-bit process
    // -----------------------------------------------------------------------------------

    /**
     * True when this process is 32-bit but the installed APK only shipped arm64
     * native libs (typical of the -arm64-v8a release on a 32-bit userspace device).
     */
    @JvmStatic
    fun isArm64OnlyOn32BitProcess(context: Context): Boolean {
        if (!isProcess32Bit()) return false
        return isInstalledApkArm64Only(context)
    }

    private fun isProcess32Bit(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            !Process.is64Bit()
        } else {
            Build.SUPPORTED_64_BIT_ABIS.isEmpty()
        }
    }

    /**
     * Inspects the installed APK zip entries directly to check if it only contains
     * arm64-v8a native libraries without any 32-bit (armeabi/armeabi-v7a) libraries.
     */
    private fun isInstalledApkArm64Only(context: Context): Boolean {
        return try {
            val apkPath = context.applicationInfo.sourceDir ?: return false
            var hasArm64 = false
            var hasArm32 = false
            ZipFile(File(apkPath)).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name.lowercase()
                    if (name.startsWith("lib/arm64") || name.contains("arm64-v8a")) {
                        hasArm64 = true
                    }
                    if (name.startsWith("lib/armeabi") || name.contains("armeabi-v7a") || name.contains("armeabi")) {
                        hasArm32 = true
                    }
                }
            }
            hasArm64 && !hasArm32
        } catch (e: Exception) {
            Log.w(TAG, "Failed to inspect APK zip entries", e)
            try {
                val libDir = context.applicationInfo.nativeLibraryDir ?: return false
                val path = libDir.lowercase()
                path.contains("arm64") && !path.contains("armeabi")
            } catch (_: Exception) {
                false
            }
        }
    }

    /**
     * Shows a non-cancelable warning on app startup when the wrong ABI variant is installed.
     * The prompt cannot be skipped or ignored since the app is missing required native libraries.
     */
    @JvmStatic
    fun maybeWarnWrongAbiVariant(activity: Activity): Boolean {
        if (isAbiWarningShowing || isDownloadInProgress) return true
        if (!isArm64OnlyOn32BitProcess(activity)) return false
        if (activity.isFinishing) return false

        isAbiWarningShowing = true
        MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.abi_mismatch_title)
            .setMessage(R.string.abi_mismatch_message)
            .setCancelable(false)
            .setPositiveButton(R.string.abi_mismatch_download_armv7) { _, _ ->
                isAbiWarningShowing = false
                downloadAndInstallArmv7Variant(activity)
            }
            .setOnDismissListener {
                isAbiWarningShowing = false
            }
            .show()
        return true
    }

    /** Opens the armeabi-v7a build download in the system browser. */
    @JvmStatic
    fun downloadAndInstallArmv7Variant(activity: Activity) {
        downloadAndInstallNamedVariant(
            activity,
            titleRes = R.string.abi_mismatch_downloading_title,
            preferredNameContains = listOf("armeabi-v7a"),
            preferNoicon = activity.resources.getBoolean(R.bool.hide_launcher_icon_available),
            directFileNameBuilder = { version, noicon ->
                if (noicon) "microg-$version-noicon-armeabi-v7a.apk"
                else "microg-$version-icon-armeabi-v7a.apk"
            }
        )
    }

    /** Opens the universal build download in the system browser. */
    @JvmStatic
    fun downloadAndInstallUniversalVariant(activity: Activity) {
        downloadAndInstallNamedVariant(
            activity,
            titleRes = R.string.abi_mismatch_downloading_title,
            preferredNameContains = emptyList(),
            preferNoicon = activity.resources.getBoolean(R.bool.hide_launcher_icon_available),
            directFileNameBuilder = { version, noicon ->
                if (noicon) "microg-$version-noicon.apk" else "microg-$version.apk"
            },
            requireUniversalName = true
        )
    }

    private fun downloadAndInstallNamedVariant(
        activity: Activity,
        titleRes: Int,
        preferredNameContains: List<String>,
        preferNoicon: Boolean,
        directFileNameBuilder: (version: String, noicon: Boolean) -> String,
        requireUniversalName: Boolean = false
    ) {
        ensureUpdateNotificationPermission(activity)

        var isCancelled = false

        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = true
            val padding = (16 * activity.resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(titleRes)
            .setView(progress)
            .setCancelable(false)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                isCancelled = true
            }
            .setOnCancelListener {
                isCancelled = true
            }
            .create()
        dialog.show()

        Thread {
            var resolvedUrl: String? = null

            val apiUrls = if (isOriginalMicrog) {
                listOf("$PRIMARY_RELEASES_URL?per_page=15")
            } else {
                listOf("$PRIMARY_RELEASES_URL?per_page=15", "$RELEASES_URL?per_page=15")
            }

            for (apiUrl in apiUrls) {
                if (resolvedUrl != null || isCancelled) break
                try {
                    val text = httpGet(apiUrl) ?: continue
                    val all = Gson().fromJson(text, JsonArray::class.java) ?: continue
                    for (el in all) {
                        val root = el.asJsonObject ?: continue
                        val assets = root.getAsJsonArray("assets") ?: continue
                        val tag = root.get("tag_name")?.asString ?: continue
                        if (!matchesReleaseTag(tag)) continue
                        val match = assets.mapNotNull { it.asJsonObject }.firstOrNull { obj ->
                            val name = obj.get("name")?.asString?.lowercase() ?: return@firstOrNull false
                            if (!matchesBuildEdition(name)) return@firstOrNull false
                            val hasNoicon = name.contains("noicon")
                            if (hasNoicon != preferNoicon) return@firstOrNull false
                            when {
                                requireUniversalName ->
                                    !name.contains("arm64-v8a") && !name.contains("armeabi-v7a")
                                preferredNameContains.isNotEmpty() ->
                                    preferredNameContains.any { name.contains(it) }
                                else -> true
                            }
                        }
                        val url = match?.get("browser_download_url")?.asString
                        if (!url.isNullOrEmpty()) {
                            resolvedUrl = url
                            break
                        }
                    }
                } catch (_: Exception) {}
            }

            val cleanVersion = BuildConfig.VERSION_NAME.trim().removePrefix("v").removePrefix("V")
                .substringBefore(" ").substringBefore("(")
            val fileName = directFileNameBuilder(cleanVersion, preferNoicon)
            val finalUrl = resolvedUrl ?: if (isOriginalMicrog) {
                PRIMARY_WEB_RELEASES_URL
            } else {
                "https://github.com/MorpheApp/MicroG-RE/releases/download/$cleanVersion/$fileName"
            }

            runOnUiThread(activity) {
                dialog.dismiss()
                if (!isCancelled) {
                    openDownloadInBrowser(activity, finalUrl)
                }
            }
        }.start()
    }

    /**
     * Makes sure the update-notification permission is granted on Android 13+.
     *
     * Older Android grants it at install time. On 13+ a background receiver can never
     * prompt, so this must be asked from a foreground activity (settings open or a
     * download action). Escalates step by step: first the system prompt, then a dialog
     * explaining notifications are required for update alerts, then a deep link to the
     * app notification settings when the user checked "don't ask again".
     */
    @JvmStatic
    fun ensureUpdateNotificationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT < 33) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (ContextCompat.checkSelfPermission(activity, permission) == PackageManager.PERMISSION_GRANTED) return

        val prefs = prefs(activity)
        val alreadyAsked = prefs.getBoolean(PREFS_NOTIF_ASKED, false)
        val canAskAgain = ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        when {
            // Never asked before: show the system prompt directly.
            !alreadyAsked -> {
                prefs.edit().putBoolean(PREFS_NOTIF_ASKED, true).apply()
                ActivityCompat.requestPermissions(
                    activity, arrayOf(permission), REQUEST_NOTIFICATION_PERMISSION
                )
            }
            // Denied once: explain why it is needed, then ask again.
            canAskAgain -> MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_notification_permission_title)
                .setMessage(R.string.update_notification_permission_message)
                .setPositiveButton(R.string.update_notification_permission_allow) { _, _ ->
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(permission), REQUEST_NOTIFICATION_PERMISSION
                    )
                }
                .setNegativeButton(R.string.update_notification_permission_not_now, null)
                .show()
            // Permanently denied ("don't ask again"): the system prompt is gone, so the
            // only remaining path is the per-app notification settings screen.
            else -> MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.update_notification_permission_title)
                .setMessage(R.string.update_notification_permission_message)
                .setPositiveButton(R.string.update_notification_permission_open_settings) { _, _ ->
                    try {
                        activity.startActivity(
                            Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, activity.packageName)
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Opening app notification settings failed", e)
                    }
                }
                .setNegativeButton(R.string.update_notification_permission_not_now, null)
                .show()
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL,
                context.getString(R.string.update_notification_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            manager.createNotificationChannel(channel)
        }
    }
}

