package com.example.doclens;

import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;

/** Sharing PDFs with other apps through the FileProvider. */
final class Files {

    private Files() {
    }

    static String authority(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    static Uri shareUri(Context context, File file) {
        return FileProvider.getUriForFile(context, authority(context), file);
    }

    /** Shares a file path or a content:// URI. */
    static void share(Context context, String path, Uri uri, String name) {
        Uri stream;
        try {
            stream = path != null ? shareUri(context, new File(path)) : uri;
        } catch (IllegalArgumentException e) {
            // Path outside the folders the FileProvider knows about
            stream = null;
        }
        if (stream == null) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("application/pdf")
                .putExtra(Intent.EXTRA_STREAM, stream)
                .putExtra(Intent.EXTRA_TITLE, name)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        send.setClipData(ClipData.newRawUri(name, stream));
        try {
            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_via)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }
}
