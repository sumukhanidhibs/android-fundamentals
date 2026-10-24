package com.example.doclens;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds every PDF on the device through MediaStore. MediaStore is the system's
 * index of shared storage, so this takes milliseconds instead of walking
 * every folder on disk.
 */
final class PdfScanner {

    private PdfScanner() {
    }

    @SuppressWarnings("deprecation") // DATA is the only column with the full path; readable with file access
    static List<PdfFile> scan(Context context) {
        List<PdfFile> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
        };
        // Some apps save PDFs without setting the MIME type, so match the extension too
        String selection = MediaStore.Files.FileColumns.MIME_TYPE + "=? OR "
                + MediaStore.Files.FileColumns.DATA + " LIKE ?";
        String[] args = {"application/pdf", "%.pdf"};

        ContentResolver resolver = context.getContentResolver();
        try (Cursor c = resolver.query(collection, projection, selection, args, null)) {
            if (c == null) return result;
            int dataCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            int nameCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME);
            int sizeCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE);
            int dateCol = c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED);
            while (c.moveToNext()) {
                String path = c.getString(dataCol);
                if (path == null || !seen.add(path)) continue;
                File file = new File(path);
                // MediaStore can still list files that were deleted a moment ago
                if (!file.isFile()) continue;

                PdfFile fromPath = PdfFile.fromPath(path, c.getLong(sizeCol), c.getLong(dateCol) * 1000);
                String name = c.getString(nameCol);
                if (name == null || name.isEmpty()) name = fromPath.name;
                long size = fromPath.size > 0 ? fromPath.size : file.length();
                long modified = fromPath.modified > 0 ? fromPath.modified : file.lastModified();
                result.add(new PdfFile(path, name, fromPath.folderPath, size, modified));
            }
        } catch (RuntimeException e) {
            // A broken MediaStore shouldn't crash the library screen; show what we have
        }
        return result;
    }
}
