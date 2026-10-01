package com.music.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

/**
 * 声波显示：5 根柱子，播放时上下波动
 * 用随机相位模拟音乐强度（不依赖 Visualizer，兼容所有设备）
 */
public class WaveformView extends View {
    private static final int BAR_COUNT = 4;
    private float[] bars = {0.3f, 0.5f, 0.7f, 0.5f, 0.3f};
    private final Random rnd = new Random();
    private boolean playing = false;
    private ValueAnimator animator;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public WaveformView(Context c) { super(c); init(); }
    public WaveformView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        paint.setColor(0xFFFFFFFF);
        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(280);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                // 用多个不同相位的正弦波模拟音乐节奏
                long t = System.currentTimeMillis();
                for (int i = 0; i < bars.length; i++) {
                    double phase = t / 180.0 + i * 1.7;
                    double v = Math.sin(phase) * 0.4 + Math.sin(phase * 2.3) * 0.3 + 0.5;
                    if (v < 0) v = 0;
                    if (v > 1) v = 1;
                    bars[i] = (float)(0.25 + v * 0.75);
                }
                invalidate();
            }
        });
    }

    public void setPlaying(boolean p) {
        if (playing == p) return;
        playing = p;
        if (p) animator.start();
        else {
            animator.cancel();
            for (int i = 0; i < bars.length; i++) bars[i] = 0.3f;
            invalidate();
        }
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int n = bars.length;
        float w = getWidth(), h = getHeight();
        float barW = w / (n * 2 - 1);
        float radius = barW / 2f;
        for (int i = 0; i < n; i++) {
            float x = i * barW * 2;
            float bh = h * bars[i];
            float by = (h - bh) / 2f;
            c.drawRoundRect(new RectF(x, by, x + barW, by + bh), radius, radius, paint);
        }
    }

    @Override protected void onDetachedFromWindow() {
        if (animator != null) animator.cancel();
        super.onDetachedFromWindow();
    }
}
