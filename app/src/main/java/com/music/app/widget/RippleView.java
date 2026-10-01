package com.music.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class RippleView extends View {
    private float progress = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public RippleView(Context c) { super(c); init(); }
    public RippleView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(0xFFFF6B9D);
    }

    public void startOnce(long d) {
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(d);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        va.start();
    }

    public void start(long d) { startOnce(d); }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float maxR = Math.max(cx, cy) * 1.3f;
        for (int i = 0; i < 3; i++) {
            float pp = Math.max(0f, Math.min(1f, progress - i * 0.15f));
            if (pp <= 0) continue;
            p.setAlpha((int)(200 * (1 - pp)));
            p.setStrokeWidth(6f * (1 - pp * 0.6f));
            c.drawCircle(cx, cy, maxR * pp, p);
        }
    }
}
