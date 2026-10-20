package com.example.unicodetonudi;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

/**
 * Small helpers around the system clipboard.
 *
 * Note: since Android 10 an app can only READ the clipboard while it is in the
 * foreground and has window focus. Writing is always allowed.
 */
final class ClipboardUtils {

    private ClipboardUtils() {
    }

    static void copy(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            clipboard.setPrimaryClip(ClipData.newPlainText(label, text));
        }
    }

    /** Returns the clipboard text, or null if the clipboard is empty or not text. */
    static String read(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            return null;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return null;
        }
        CharSequence text = clip.getItemAt(0).coerceToText(context);
        return text == null ? null : text.toString();
    }
}
