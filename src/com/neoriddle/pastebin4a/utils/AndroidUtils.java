package com.neoriddle.pastebin4a.utils;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;

/**
 * This class contains some utility methods for Android applications.
 *
 * @author Israel Buitron
 */
public class AndroidUtils {

    /**
     * Get application version code.
     *
     * @param context
     * @return Application version code.
     */
    public static int getAppVersionCode(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode;
        } catch (final NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Get application version name.
     *
     * @param context
     * @return Application version name.
     */
    public static String getAppVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (final NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
