package com.music.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class ChargePulseView extends View {
    private float phase = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator va;
    public ChargePulseView(Context c) { super(c); }
    public ChargePulseView(Context c, AttributeSet a) { super(c, a); }
    public void start() {
        if (va != null) va.cancel();
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1400);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); });
        va.start();
    }
    public void stop() {
        if (va != null) va.cancel();
        phase = 0;
        postInvalidateOnAnimation();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(4f);
        p.setColor(0xFF00FF88);
        for (int i = 0; i < 3; i++) {
            float pp = (phase + i * 0.33f) % 1f;
            p.setAlpha((int)((1 - pp) * 180));
            c.drawCircle(cx, cy, maxR * pp, p);
        }
    }
}
