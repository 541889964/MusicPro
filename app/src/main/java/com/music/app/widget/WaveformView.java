package com.music.app.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class WaveformView extends View {
    private static final int N = 4;
    private final float[] heights = new float[N];
    private final float[] targets = new float[N];
    private boolean playing = false;
    private long t0 = 0;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WaveformView(Context c) { super(c); init(); }
    public WaveformView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        for (int i = 0; i < N; i++) { heights[i] = 0.3f; targets[i] = 0.3f; }
    }

    public void setPlaying(boolean p) { playing = p; }

    /** 由外部帧循环每帧调用 */
    public void step() {
        if (!playing) {
            for (int i = 0; i < N; i++) targets[i] = 0.3f;
        } else {
            long t = System.currentTimeMillis() - t0;
            for (int i = 0; i < N; i++) {
                double phase = t / 200.0 + i * 1.4;
                double v = 0.4 + 0.6 * Math.abs(
                    Math.sin(phase) * 0.5 + Math.sin(phase * 2.3 + i) * 0.5);
                if (v < 0.15) v = 0.15;
                if (v > 1.0) v = 1.0;
                targets[i] = (float) v;
            }
        }
        boolean need = false;
        for (int i = 0; i < N; i++) {
            float d = targets[i] - heights[i];
            heights[i] += d * 0.35f;
            if (Math.abs(d) > 0.01f) need = true;
        }
        if (need || playing) invalidate();
    }

    public void start() {
        t0 = System.currentTimeMillis();
        playing = true;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        float gap = w * 0.12f;
        float barW = (w - gap * (N - 1)) / N;
        float radius = barW / 2f;
        // 渐变
        paint.setShader(new LinearGradient(0, 0, 0, h,
            0xFFFFFFFF, 0x88FFFFFF, Shader.TileMode.CLAMP));
        for (int i = 0; i < N; i++) {
            float x = i * (barW + gap);
            float bh = h * heights[i];
            float by = (h - bh) / 2f;
            c.drawRoundRect(new RectF(x, by, x + barW, by + bh),
                radius, radius, paint);
        }
    }
}
