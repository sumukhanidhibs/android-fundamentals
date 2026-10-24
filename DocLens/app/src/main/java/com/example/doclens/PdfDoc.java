package com.example.doclens;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BooleanSupplier;

/**
 * An open PDF, rendered with Android's built-in PdfRenderer (PDFium).
 * PdfRenderer can only work on one page at a time, so every page is
 * rendered on a single background thread, in the order requested.
 */
final class PdfDoc implements Closeable {

    interface Callback {
        /** Called on the main thread; {@code bitmap} is null if rendering failed or was skipped. */
        void onRendered(int page, int width, Bitmap bitmap, float aspect);
    }

    /** Keeps a single page bitmap under ~32 MB, also for very tall pages. */
    private static final long MAX_PIXELS = 8_000_000L;

    final int pageCount;
    /** Height / width of the first page, used for pages that haven't been measured yet. */
    final float firstAspect;

    private final ParcelFileDescriptor fd;
    private final PdfRenderer renderer;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile boolean closed;

    /**
     * Opens the renderer. Call on a background thread.
     *
     * @throws SecurityException if the PDF is password protected
     * @throws IOException       if it isn't a readable PDF
     */
    PdfDoc(ParcelFileDescriptor fd) throws IOException {
        this.fd = fd;
        PdfRenderer r;
        try {
            r = new PdfRenderer(fd);
        } catch (IOException | RuntimeException e) {
            fd.close();
            throw e;
        }
        renderer = r;
        pageCount = renderer.getPageCount();
        if (pageCount == 0) {
            close();
            throw new IOException("PDF has no pages");
        }
        try (PdfRenderer.Page first = renderer.openPage(0)) {
            firstAspect = aspectOf(first);
        } catch (RuntimeException e) {
            close();
            throw new IOException("Unreadable first page", e);
        }
    }

    void render(int page, int width, BooleanSupplier stillWanted, Callback callback) {
        if (closed) return;
        worker.execute(() -> {
            Bitmap bitmap = null;
            float aspect = -1;
            if (!closed && stillWanted.getAsBoolean()) {
                try (PdfRenderer.Page p = renderer.openPage(page)) {
                    aspect = aspectOf(p);
                    int w = width;
                    int h = Math.max(1, Math.round(w * aspect));
                    long pixels = (long) w * h;
                    if (pixels > MAX_PIXELS) {
                        double k = Math.sqrt(MAX_PIXELS / (double) pixels);
                        w = Math.max(1, (int) (w * k));
                        h = Math.max(1, (int) (h * k));
                    }
                    bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                    bitmap.eraseColor(Color.WHITE);
                    p.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                } catch (RuntimeException | OutOfMemoryError e) {
                    bitmap = null;
                }
            }
            final Bitmap result = bitmap;
            final float measured = aspect;
            main.post(() -> callback.onRendered(page, width, result, measured));
        });
    }

    private static float aspectOf(PdfRenderer.Page page) {
        return page.getWidth() > 0 && page.getHeight() > 0
                ? page.getHeight() / (float) page.getWidth()
                : 1.414f;
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        // Runs after any page that is rendering right now
        worker.execute(() -> {
            try {
                renderer.close();
            } catch (RuntimeException ignored) {
            }
            try {
                fd.close();
            } catch (IOException ignored) {
            }
        });
        worker.shutdown();
    }
}
