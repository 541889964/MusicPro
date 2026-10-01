package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class AuroraView extends View {
    private float phase = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private ValueAnimator va;
    public AuroraView(Context c) { super(c); }
    public AuroraView(Context c, AttributeSet a) { super(c, a); }
    public void start() {
        if (va != null) va.cancel();
        va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(6000);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.addUpdateListener(a -> { phase = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); });
        va.start();
    }
    public void stop() { if (va != null) va.cancel(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int w = getWidth(), h = getHeight();
        p.setShader(new LinearGradient(0, 0, w, h,
            new int[]{0x409B6BFF, 0x66FF6B9D, 0x4067E8F9, 0x409B6BFF},
            null, Shader.TileMode.CLAMP));
        path.reset();
        path.moveTo(0, h);
        for (int i = 0; i <= 40; i++) {
            float x = w * i / 40f;
            float y = h * 0.6f
                + (float)Math.sin(i * 0.4 + phase * 6.28) * h * 0.12f
                + (float)Math.cos(i * 0.7 + phase * 4.71) * h * 0.08f;
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.close();
        c.drawPath(path, p);
        p.setShader(null);
    }
}
