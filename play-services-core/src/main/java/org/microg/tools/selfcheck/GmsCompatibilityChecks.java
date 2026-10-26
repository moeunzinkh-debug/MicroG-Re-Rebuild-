/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.tools.selfcheck;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.google.android.gms.R;

import org.microg.gms.utils.GmsVendorDetector;
import org.microg.tools.ui.AbstractSelfCheckFragment.ChipInfo;

import java.util.ArrayList;
import java.util.List;

import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Advisory;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Neutral;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

/**
 * Explains which GMS implementation this build answers as and whether every patched app on the
 * device is patched for a GMS implementation that is actually installed.
 *
 * Patched apps are permanently bound to a single GMS package name, so this is the only thing a
 * standalone build can do about mismatches: detect them, and tell the user exactly which package
 * the app expects and how to fix it (install that build, or re-patch with this build's vendor id).
 *
 * Informational rows are reported as {@link Result#Advisory} with {@code showIcon == false} so that
 * their explanation text stays visible without turning the whole Self-Check red.
 */
public class GmsCompatibilityChecks implements SelfCheckGroup {

    private static final String TAG = "MicroG-Compat";

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_gms_compat);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        String ownPackage = context.getPackageName();
        String ownVendor = vendorDisplayName(context, ownPackage);
        String ownName = ownVendor != null
                ? context.getString(R.string.gms_vendor_with_package, ownVendor, ownPackage)
                : ownPackage;

        collector.addResult(
                context.getString(R.string.self_check_gms_build_is, ownName),
                Advisory,
                context.getString(R.string.self_check_gms_build_is_resolution, ownPackage),
                false,
                null,
                null
        );

        List<GmsVendorDetector.Implementation> implementations =
                GmsVendorDetector.INSTANCE.installedImplementations(context);

        List<ChipInfo> implementationChips = new ArrayList<>();
        StringBuilder installed = new StringBuilder();
        for (GmsVendorDetector.Implementation implementation : implementations) {
            String vendorName = vendorDisplayName(context, implementation.getPackageName());
            String label = vendorName != null ? vendorName : implementation.getPackageName();
            String versionName = implementation.getVersionName();
            if (versionName != null && !versionName.isEmpty()) {
                label = label + " " + versionName;
            }
            if (installed.length() > 0) installed.append(", ");
            installed.append(label);
            implementationChips.add(new ChipInfo(label,
                    appIcon(context, implementation.getPackageName()),
                    openAppInfo(context, implementation.getPackageName())));
        }

        collector.addResult(
                context.getString(R.string.self_check_gms_impl_installed),
                Advisory,
                context.getString(R.string.self_check_gms_impl_resolution, installed.toString()),
                false,
                implementationChips,
                null
        );

        List<GmsVendorDetector.PatchedApp> patchedApps;
        try {
            patchedApps = GmsVendorDetector.INSTANCE.patchedApps(context);
        } catch (Throwable t) {
            Log.w(TAG, "Patched app detection failed", t);
            patchedApps = new ArrayList<>();
        }

        if (patchedApps.isEmpty()) {
            collector.addResult(
                    context.getString(R.string.self_check_gms_no_patched_apps),
                    Neutral,
                    ""
            );
            return;
        }

        for (GmsVendorDetector.PatchedApp app : patchedApps) {
            String appLabel = app.getLabel() != null ? app.getLabel() : app.getPackageName();
            String expected = app.getExpectedVendor();
            List<ChipInfo> chips = new ArrayList<>();
            chips.add(new ChipInfo(appLabel, appIcon(context, app.getPackageName()),
                    openAppInfo(context, app.getPackageName())));

            if (expected == null) {
                collector.addResult(
                        context.getString(R.string.self_check_gms_app_unknown, appLabel),
                        Advisory,
                        context.getString(R.string.self_check_gms_app_unknown_resolution, appLabel, ownPackage),
                        false,
                        chips,
                        fragment -> launchAppInfo(fragment.getContext(), app.getPackageName())
                );
                continue;
            }

            String expectedName = vendorDisplayName(context, expected);
            String expectedLabel = expectedName != null ? expectedName : expected;
            boolean isInstalled = GmsVendorDetector.INSTANCE.isInstalled(context, expected);
            boolean isOwnVendor = expected.equals(ownPackage);

            if (isInstalled) {
                collector.addResult(
                        context.getString(isOwnVendor
                                        ? R.string.self_check_gms_app_ok
                                        : R.string.self_check_gms_app_ok_other,
                                appLabel, expectedLabel),
                        Positive,
                        "",
                        false,
                        chips,
                        null
                );
            } else {
                collector.addResult(
                        context.getString(R.string.self_check_gms_app_missing, appLabel, expectedLabel),
                        Negative,
                        context.getString(R.string.self_check_gms_app_missing_resolution,
                                expectedLabel, expected, appLabel, ownPackage),
                        true,
                        chips,
                        fragment -> launchAppInfo(fragment.getContext(), app.getPackageName())
                );
            }
        }
    }

    private static View.OnClickListener openAppInfo(Context context, String packageName) {
        return v -> launchAppInfo(context, packageName);
    }

    private static void launchAppInfo(Context context, String packageName) {
        if (context == null) return;
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.w(TAG, "Cannot open app info for " + packageName, e);
        }
    }

    private static Drawable appIcon(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            return ContextCompat.getDrawable(context, R.drawable.ic_information);
        }
    }

    private static String vendorDisplayName(Context context, String packageName) {
        if (packageName == null) return null;
        switch (packageName) {
            case "app.revanced.android.gms":
                return context.getString(R.string.gms_vendor_revanced);
            case "app.morphe.android.gms":
                return context.getString(R.string.gms_vendor_morphe);
            case "org.microg.android.gms":
                return context.getString(R.string.gms_vendor_personal);
            case "org.microg.gms":
                return context.getString(R.string.gms_vendor_user);
            case "app.rvx.android.gms":
                return context.getString(R.string.gms_vendor_rvx);
            case "com.mgoogle.android.gms":
                return context.getString(R.string.gms_vendor_mgoogle);
            case "com.google.android.gms":
                return context.getString(R.string.gms_vendor_google);
            default:
                return null;
        }
    }
}
