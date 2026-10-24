package com.example.doclens;

import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shows the pages of a PdfDoc. Pages are rendered on demand at the width
 * of the screen (2× when zoomed in) and kept in a memory cache, so
 * scrolling back is instant.
 */
final class PageAdapter extends RecyclerView.Adapter<PageAdapter.Holder> {

    private static final Object PAYLOAD_QUALITY = new Object();

    /** Inverts colours but keeps the page readable: white paper → dark, black text → light. */
    private static final ColorMatrixColorFilter NIGHT_FILTER = new ColorMatrixColorFilter(new ColorMatrix(new float[]{
            -0.9f, 0, 0, 0, 240,
            0, -0.9f, 0, 0, 240,
            0, 0, -0.9f, 0, 240,
            0, 0, 0, 1, 0,
    }));

    private final PdfDoc doc;
    private final float[] aspects;
    private final LruCache<String, Bitmap> cache;
    /** Pages currently on screen (or about to be); read by the render thread. */
    private final Set<Integer> wanted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> pending = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private int baseWidth;
    private float quality = 1f;
    private boolean night;

    PageAdapter(PdfDoc doc) {
        this.doc = doc;
        aspects = new float[doc.pageCount];
        Arrays.fill(aspects, doc.firstAspect);
        int cacheBytes = (int) Math.min(Runtime.getRuntime().maxMemory() / 4, Integer.MAX_VALUE);
        cache = new LruCache<String, Bitmap>(cacheBytes) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount();
            }
        };
    }

    /** Width in pixels of a page at 100% zoom. */
    void setBaseWidth(int width) {
        if (width <= 0 || width == baseWidth) return;
        baseWidth = width;
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_QUALITY);
    }

    /** Re-renders visible pages sharper after zooming in (or smaller after zooming out). */
    void setZoom(float zoom) {
        float q = zoom > 1.4f ? 2f : 1f;
        if (q == quality) return;
        quality = q;
        if (q == 1f) cache.evictAll();
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_QUALITY);
    }

    void setNight(boolean night) {
        if (this.night == night) return;
        this.night = night;
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_QUALITY);
    }

    @Override
    public int getItemCount() {
        return doc.pageCount;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_page, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position, @NonNull List<Object> payloads) {
        // Same page, new quality: keep the current bitmap on screen until the new one is ready
        bind(holder, position, !payloads.isEmpty() && holder.page == position);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        bind(holder, position, false);
    }

    private void bind(Holder holder, int position, boolean keepBitmap) {
        if (holder.page != position) wanted.remove(holder.page);
        holder.page = position;
        wanted.add(position);
        holder.view.setAspect(aspects[position]);
        holder.view.setColorFilter(night ? NIGHT_FILTER : null);
        holder.view.setBackgroundColor(ContextCompat.getColor(holder.view.getContext(),
                night ? R.color.page_placeholder_night : R.color.page_placeholder));
        holder.view.setContentDescription(holder.view.getContext()
                .getString(R.string.page_indicator, position + 1, doc.pageCount));

        int width = renderWidth();
        if (width <= 0) return;
        Bitmap bitmap = cache.get(key(position, width));
        if (bitmap != null) {
            holder.view.setImageBitmap(bitmap);
            return;
        }
        if (!keepBitmap) {
            // A lower quality copy is better than an empty page while rendering
            Bitmap fallback = cache.get(key(position, baseWidth));
            holder.view.setImageBitmap(fallback);
        }
        request(position, width);
    }

    @Override
    public void onViewRecycled(@NonNull Holder holder) {
        wanted.remove(holder.page);
        holder.page = RecyclerView.NO_POSITION;
        holder.view.setImageBitmap(null);
    }

    private int renderWidth() {
        return Math.round(baseWidth * quality);
    }

    private static String key(int page, int width) {
        return page + "@" + width;
    }

    private void request(int page, int width) {
        String key = key(page, width);
        if (!pending.add(key)) return;
        doc.render(page, width, () -> wanted.contains(page), (p, w, bitmap, aspect) -> {
            pending.remove(key(p, w));
            if (aspect > 0 && Math.abs(aspects[p] - aspect) > 0.001f) {
                aspects[p] = aspect;
                notifyItemChanged(p, PAYLOAD_QUALITY);
            }
            if (bitmap == null) {
                if (aspect > 0) cache.trimToSize(cache.maxSize() / 2); // likely out of memory
                return;
            }
            cache.put(key(p, w), bitmap);
            if (w == renderWidth()) notifyItemChanged(p, PAYLOAD_QUALITY);
        });
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final PageView view;
        int page = RecyclerView.NO_POSITION;

        Holder(View itemView) {
            super(itemView);
            view = itemView.findViewById(R.id.page);
        }
    }
}
