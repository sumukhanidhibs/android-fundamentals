package com.example.doclens;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.content.ContextCompat;

/**
 * Listing PDFs that other apps saved needs broad file access:
 *   Android 11+  → "All files access" (MANAGE_EXTERNAL_STORAGE), granted in Settings
 *   Android 10-  → READ_EXTERNAL_STORAGE runtime permission
 */
final class StorageAccess {

    private StorageAccess() {
    }

    static boolean has(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        }
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;
    }

    /** True when access is granted on a Settings screen instead of a runtime dialog. */
    static boolean usesSettingsScreen() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    static void openSettingsScreen(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return;
        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:" + context.getPackageName()));
        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Some devices only have the list of all apps
            context.startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
        }
    }
}
