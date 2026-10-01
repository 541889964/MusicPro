package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

public class ChargePulseView extends View {
    private float phase = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private ValueAnimator va;

    public ChargePulseView(Context c) { super(c); }
    public ChargePulseView(Context c, AttributeSet a) { super(c, a); }

    public void start() {
        stop();
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1400);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        va.start();
    }

    public void stop() {
        if (va != null) va.cancel();
        phase = 0f;
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth() / 2f, cy = getHeight() / 2f;
        float maxR = Math.min(cx, cy);
        // 三圈绿色光波
        for (int i = 0; i < 3; i++) {
            float pp = (phase + i * 0.33f) % 1f;
            float r = maxR * pp;
            float alpha = (1 - pp) * 0.6f;
            p.setShader(new RadialGradient(cx, cy, Math.max(r, 1f),
                new int[]{(0x00 << 24) | 0x00FF88, (int)(alpha * 255) << 24 | 0x00FF88},
                null, Shader.TileMode.CLAMP));
            c.drawCircle(cx, cy, r, p);
        }
        p.setShader(null);
    }
}
