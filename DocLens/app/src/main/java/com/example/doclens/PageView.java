package com.example.doclens;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/** A PDF page: always as tall as the page's aspect ratio says, even before it is rendered. */
public class PageView extends AppCompatImageView {

    private float aspect = 1.414f; // A4

    public PageView(Context context) {
        super(context);
    }

    public PageView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void setAspect(float aspect) {
        if (aspect <= 0 || Math.abs(aspect - this.aspect) < 0.001f) return;
        this.aspect = aspect;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, Math.round(width * aspect));
    }
}
