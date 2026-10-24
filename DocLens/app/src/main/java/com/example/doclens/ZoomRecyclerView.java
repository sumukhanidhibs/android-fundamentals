package com.example.doclens;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * A vertical RecyclerView with pinch-to-zoom, double-tap zoom and panning.
 *
 * The list itself is never resized: the whole content is drawn scaled by
 * {@code scale} and shifted by (tx, ty). At 100% the RecyclerView scrolls as
 * usual. Zoomed in, drags pan the zoomed view first and scroll the list once
 * the edge of the zoomed viewport is reached.
 */
public class ZoomRecyclerView extends RecyclerView {

    public interface Listener {
        void onTap();

        /** Called when a pinch or double-tap zoom finishes. */
        void onZoomChanged(float scale);
    }

    private static final float MAX_SCALE = 5f;
    private static final float DOUBLE_TAP_SCALE = 2.5f;

    private final ScaleGestureDetector scaleDetector;
    private final GestureDetector gestureDetector;

    private float scale = 1f;
    private float tx;
    private float ty;
    private float scrollRemainder;
    /** The current gesture pans/zooms instead of scrolling the list. */
    private boolean zoomGesture;
    private ValueAnimator animator;
    private Listener listener;

    public ZoomRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ZoomRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        scaleDetector = new ScaleGestureDetector(context, new ScaleListener());
        gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public float getScale() {
        return scale;
    }

    /** Back to 100%. */
    public void resetZoom() {
        cancelAnimation();
        boolean changed = scale != 1f;
        scale = 1f;
        tx = 0;
        ty = 0;
        invalidate();
        if (changed && listener != null) listener.onZoomChanged(scale);
    }

    /** The child under the centre of the screen, taking the zoom into account. */
    @Nullable
    public View findChildAtCenter() {
        float x = (getWidth() / 2f - tx) / scale;
        float y = (getHeight() / 2f - ty) / scale;
        return findChildViewUnder(x, y);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (oldw != 0 && (w != oldw || h != oldh)) resetZoom();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        canvas.save();
        canvas.translate(tx, ty);
        canvas.scale(scale, scale);
        super.dispatchDraw(canvas);
        canvas.restore();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        // Pages are not clickable, but take every gesture while zoomed or pinching
        if (scale > 1f || e.getPointerCount() > 1) return true;
        return super.onInterceptTouchEvent(e);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN) {
            cancelAnimation();
            zoomGesture = scale > 1f;
            if (zoomGesture) stopScroll();
        }
        scaleDetector.onTouchEvent(e);
        gestureDetector.onTouchEvent(e);

        if (!zoomGesture && (scaleDetector.isInProgress() || e.getPointerCount() > 1)) {
            // A pinch started while the list was scrolling: take over from it
            zoomGesture = true;
            MotionEvent cancel = MotionEvent.obtain(e);
            cancel.setAction(MotionEvent.ACTION_CANCEL);
            super.onTouchEvent(cancel);
            cancel.recycle();
        }
        if (zoomGesture) {
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                zoomGesture = false;
            }
            return true;
        }
        return super.onTouchEvent(e);
    }

    private void pan(float dx, float dy) {
        tx = clamp(tx - dx, getWidth() * (1 - scale), 0);

        float newTy = ty - dy;
        float minTy = getHeight() * (1 - scale);
        if (newTy > 0) {
            scrollList(-newTy / scale);
            newTy = 0;
        } else if (newTy < minTy) {
            scrollList((minTy - newTy) / scale);
            newTy = minTy;
        }
        ty = newTy;
        invalidate();
    }

    private void scrollList(float dy) {
        scrollRemainder += dy;
        int whole = (int) scrollRemainder;
        scrollRemainder -= whole;
        if (whole != 0) scrollBy(0, whole);
    }

    /** Zooms to {@code newScale}, keeping the point (fx, fy) on screen where it is. */
    private void zoomTo(float newScale, float fx, float fy) {
        newScale = clamp(newScale, 1f, MAX_SCALE);
        float f = newScale / scale;
        tx = fx - (fx - tx) * f;
        ty = fy - (fy - ty) * f;
        scale = newScale;
        tx = clamp(tx, getWidth() * (1 - scale), 0);
        ty = clamp(ty, getHeight() * (1 - scale), 0);
        invalidate();
    }

    private void animateZoom(float target, float fx, float fy) {
        cancelAnimation();
        animator = ValueAnimator.ofFloat(scale, target);
        animator.setDuration(220);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> zoomTo((float) a.getAnimatedValue(), fx, fy));
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (listener != null) listener.onZoomChanged(scale);
            }
        });
        animator.start();
    }

    private void cancelAnimation() {
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private final class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector d) {
            zoomTo(scale * d.getScaleFactor(), d.getFocusX(), d.getFocusY());
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector d) {
            if (scale < 1.05f) {
                animateZoom(1f, d.getFocusX(), d.getFocusY());
            } else if (listener != null) {
                listener.onZoomChanged(scale);
            }
        }
    }

    private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (listener != null) listener.onTap();
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            animateZoom(scale > 1.01f ? 1f : DOUBLE_TAP_SCALE, e.getX(), e.getY());
            return true;
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
            if (!zoomGesture || scale <= 1f) return false;
            pan(dx, dy);
            return true;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float vx, float vy) {
            if (!zoomGesture || scale <= 1f || scaleDetector.isInProgress()) return false;
            fling(0, (int) (-vy / scale));
            return true;
        }
    }
}
