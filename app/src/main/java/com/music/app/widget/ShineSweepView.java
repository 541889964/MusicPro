package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class ShineSweepView extends View {
    private float x = -1f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public ShineSweepView(Context c) { super(c); }
    public ShineSweepView(Context c, AttributeSet a) { super(c, a); }
    public void sweep() {
        ValueAnimator va = ValueAnimator.ofFloat(-1f, 2f);
        va.setDuration(900);
        va.addUpdateListener(a -> { x = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); });
        va.start();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (x < -0.5f || x > 1.5f) return;
        float w = getWidth(), h = getHeight();
        float bandW = w * 0.2f;
        float cx = x * w;
        p.setShader(new LinearGradient(cx - bandW, 0, cx + bandW, 0,
            new int[]{0x00FFFFFF, 0x88FFFFFF, 0x00FFFFFF}, null, Shader.TileMode.CLAMP));
        c.drawRect(cx - bandW, 0, cx + bandW, h, p);
        p.setShader(null);
    }
}
