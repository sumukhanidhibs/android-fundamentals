package com.example.doclens;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Android never lets an app make itself the default; the user has to pick it.
 * This helper finds out who opens PDFs today and walks the user through it:
 *
 *   no default yet      → open a small welcome PDF, the system asks
 *                          "Open with…" and the user taps DocLens → Always
 *   another app default → send the user to that app's "Open by default"
 *                          settings to clear it first
 */
final class DefaultViewer {

    private DefaultViewer() {
    }

    private static Intent probeIntent(Context context) {
        Uri probe = Uri.parse("content://" + Files.authority(context) + "/probe.pdf");
        return new Intent(Intent.ACTION_VIEW).setDataAndType(probe, "application/pdf");
    }

    /** Package that opens PDFs without asking, or null when the system shows a chooser. */
    static String defaultPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent intent = probeIntent(context);
        ResolveInfo resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (resolved == null || resolved.activityInfo == null) return null;
        String pkg = resolved.activityInfo.packageName;
        // When there's no default, resolveActivity returns the system chooser,
        // which isn't one of the apps that can actually open PDFs.
        List<ResolveInfo> candidates = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo candidate : candidates) {
            if (candidate.activityInfo != null && pkg.equals(candidate.activityInfo.packageName)) {
                return pkg;
            }
        }
        return null;
    }

    static boolean isDefault(Context context) {
        return context.getPackageName().equals(defaultPackage(context));
    }

    static void makeDefault(Activity activity) {
        String current = defaultPackage(activity);
        if (activity.getPackageName().equals(current)) {
            Toast.makeText(activity, R.string.default_done, Toast.LENGTH_SHORT).show();
            return;
        }
        if (current == null) {
            openWelcomePdf(activity);
            return;
        }
        String label = appLabel(activity, current);
        new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.default_title)
                .setMessage(activity.getString(R.string.default_clear_other, label))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(label, (d, w) -> {
                    Intent settings = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.parse("package:" + current));
                    try {
                        activity.startActivity(settings);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(activity, R.string.no_app_found, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private static String appLabel(Context context, String pkg) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(info).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    /** Opens a one-page PDF with ACTION_VIEW so the system shows its "Open with / Always" sheet. */
    private static void openWelcomePdf(Activity activity) {
        File pdf;
        try {
            pdf = writeWelcomePdf(activity);
        } catch (IOException e) {
            Toast.makeText(activity, R.string.error_open, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent view = new Intent(Intent.ACTION_VIEW)
                .setDataAndType(Files.shareUri(activity, pdf), "application/pdf")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            activity.startActivity(view);
            Toast.makeText(activity, R.string.default_pick_always, Toast.LENGTH_LONG).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.no_app_found, Toast.LENGTH_SHORT).show();
        }
    }

    private static File writeWelcomePdf(Context context) throws IOException {
        File file = new File(context.getCacheDir(), "DocLens.pdf");
        PdfDocument doc = new PdfDocument();
        PdfDocument.Page page = doc.startPage(new PdfDocument.PageInfo.Builder(595, 842, 1).create());
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        paint.setColor(Color.rgb(0x13, 0x1B, 0x30));
        canvas.drawRect(0, 0, 595, 260, paint);
        paint.setColor(Color.rgb(0xE0, 0x26, 0x2E));
        canvas.drawRoundRect(237, 70, 357, 190, 24, 24, paint);
        paint.setColor(Color.WHITE);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(36);
        canvas.drawText("PDF", 297, 143, paint);

        paint.setColor(Color.rgb(0x13, 0x1B, 0x30));
        paint.setTextSize(34);
        canvas.drawText(context.getString(R.string.welcome_pdf_title), 297, 360, paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(20);
        paint.setColor(Color.rgb(0x59, 0x61, 0x78));
        canvas.drawText(context.getString(R.string.welcome_pdf_body), 297, 400, paint);

        doc.finishPage(page);
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.writeTo(out);
        } finally {
            doc.close();
        }
        return file;
    }
}
