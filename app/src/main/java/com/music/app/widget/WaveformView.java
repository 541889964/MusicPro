package com.music.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class WaveformView extends View {
    private static final int N = 4;
    private final float[] bars = {0.3f, 0.5f, 0.7f, 0.5f};
    private boolean playing = false;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private long t0 = 0;
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!playing) return;
            long t = System.currentTimeMillis() - t0;
            for (int i = 0; i < N; i++) {
                double phase = t / 220.0 + i * 1.5;
                double v = 0.5 + 0.5 * (Math.sin(phase) * 0.5 + Math.sin(phase * 2.1) * 0.5);
                if (v < 0.15) v = 0.15;
                if (v > 1.0) v = 1.0;
                bars[i] = (float) v;
            }
            postInvalidateOnAnimation();
            postDelayed(this, 33);
        }
    };

    public WaveformView(Context c) { super(c); init(); }
    public WaveformView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        paint.setColor(0xFFFFFFFF);
    }

    public void setPlaying(boolean p) {
        if (playing == p) return;
        playing = p;
        if (p) {
            t0 = System.currentTimeMillis();
            removeCallbacks(tick);
            post(tick);
        } else {
            removeCallbacks(tick);
            for (int i = 0; i < N; i++) bars[i] = 0.3f;
            invalidate();
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        float gap = w * 0.12f;
        float barW = (w - gap * (N - 1)) / N;
        float radius = barW / 2f;
        for (int i = 0; i < N; i++) {
            float x = i * (barW + gap);
            float bh = h * bars[i];
            float by = (h - bh) / 2f;
            c.drawRoundRect(new RectF(x, by, x + barW, by + bh), radius, radius, paint);
        }
    }

    @Override protected void onDetachedFromWindow() {
        playing = false;
        removeCallbacks(tick);
        super.onDetachedFromWindow();
    }
}
