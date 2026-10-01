package com.music.app.widget;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
public class RingProgressView extends View {
    float progress = 0f;
    final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Paint fg = new Paint(Paint.ANTI_ALIAS_FLAG);
    final RectF r = new RectF();
    public RingProgressView(Context c){ super(c); init(); }
    public RingProgressView(Context c, AttributeSet a){ super(c,a); init(); }
    void init() {
        bg.setStyle(Paint.Style.STROKE); bg.setStrokeWidth(6f); bg.setColor(0x33FFFFFF);
        fg.setStyle(Paint.Style.STROKE); fg.setStrokeWidth(6f);
        fg.setStrokeCap(Paint.Cap.ROUND);
        fg.setShader(new LinearGradient(0,0,100,100, 0xFF9B6BFF, 0xFFFF6B9D, Shader.TileMode.CLAMP));
    }
    public void setProgress(float p) { this.progress = p; postInvalidateOnAnimation(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float pad = 12f;
        r.set(pad, pad, getWidth()-pad, getHeight()-pad);
        c.drawArc(r, 0, 360, false, bg);
        c.drawArc(r, -90, progress, false, fg);
    }
}
