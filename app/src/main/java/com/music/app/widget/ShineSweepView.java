package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class ShineSweepView extends View {
    private float x = -1f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ShineSweepView(Context c) { super(c); }
    public ShineSweepView(Context c, AttributeSet a) { super(c, a); }

    public void sweep() {
        ValueAnimator va = ValueAnimator.ofFloat(-1f, 1f);
        va.setDuration(700);
        va.setInterpolator(new LinearInterpolator());
        va.addUpdateListener(a -> {
            x = (float) a.getAnimatedValue();
            postInvalidateOnAnimation();
        });
        va.start();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (x < -0.5f || x > 0.5f) return;
        float w = getWidth(), h = getHeight();
        float bandW = w * 0.15f;
        float centerX = (x + 0.5f) * w;
        p.setShader(new LinearGradient(
            centerX - bandW, 0, centerX + bandW, 0,
            new int[]{0x00FFFFFF, 0x88FFFFFF, 0x00FFFFFF},
            null, Shader.TileMode.CLAMP));
        c.drawRect(centerX - bandW, 0, centerX + bandW, h, p);
        p.setShader(null);
    }
}
