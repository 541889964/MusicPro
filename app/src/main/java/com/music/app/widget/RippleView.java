package com.music.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;

public class RippleView extends View {
    private float progress = 0f;
    private int color = 0xFFFF6B9D;
    private boolean looping = false;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RippleView(Context c) { super(c); init(); }
    public RippleView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() { p.setStyle(Paint.Style.STROKE); }

    public void setColor(int c) { this.color = c; }

    /** 单次播放 */
    public void startOnce(long dur) {
        stop();
        looping = false;
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(dur);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                progress = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            }
        });
        va.start();
    }

    /** 兼容旧调用：start(long) */
    public void start(long dur) { startOnce(dur); }

    /** 无限循环 */
    public void startLoop() {
        stop();
        looping = true;
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1800);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                progress = (float) a.getAnimatedValue();
                postInvalidateOnAnimation();
            }
        });
        va.start();
    }

    public void stop() {
        looping = false;
        progress = 0f;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float maxR = Math.max(cx, cy) * 1.4f;
        for (int i = 0; i < 3; i++) {
            float pp = (progress + i * 0.18f) % 1f;
            if (pp <= 0f) continue;
            p.setColor(color);
            p.setAlpha((int)(180 * (1 - pp)));
            p.setStrokeWidth(5f * (1 - pp * 0.7f));
            c.drawCircle(cx, cy, maxR * pp, p);
        }
    }
}
