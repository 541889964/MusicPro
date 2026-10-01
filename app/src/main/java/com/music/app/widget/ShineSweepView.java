package com.music.app.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

public class ShineSweepView extends View {
    private float x = -1f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private LinearGradient grad;
    private final Matrix mtx = new Matrix();

    public ShineSweepView(Context c) { super(c); }
    public ShineSweepView(Context c, AttributeSet a) { super(c, a); }

    public void sweep() {
        ValueAnimator va = ValueAnimator.ofFloat(-1f, 2f);
        va.setDuration(900);
        va.addUpdateListener(a -> { x = (float) a.getAnimatedValue(); postInvalidateOnAnimation(); });
        va.start();
    }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        grad = null;
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (x < -0.5f || x > 1.5f) return;
        float w = getWidth(), h = getHeight();
        float bandW = w * 0.2f;
        if (grad == null) {
            grad = new LinearGradient(0, 0, bandW * 2, 0,
                new int[]{0x00FFFFFF, 0x88FFFFFF, 0x00FFFFFF},
                null, Shader.TileMode.CLAMP);
        }
        mtx.reset();
        mtx.setTranslate(x * w - bandW, 0);
        grad.setLocalMatrix(mtx);
        p.setShader(grad);
        c.drawRect(x * w - bandW, 0, x * w + bandW, h, p);
    }
}
