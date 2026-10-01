package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
public class RippleView extends View {
    private float progress = 0f;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public RippleView(Context c){ super(c); init(); }
    public RippleView(Context c, AttributeSet a){ super(c,a); init(); }
    private void init(){
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(4f);
        paint.setColor(Color.parseColor("#FFB6D0"));
    }
    public void start(long d) {
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(d); a.setInterpolator(new DecelerateInterpolator());
        a.addUpdateListener(an -> { progress = (float) an.getAnimatedValue(); invalidate(); });
        a.start();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth()/2f, cy = getHeight()/2f;
        float maxR = Math.min(cx, cy);
        for (int i = 0; i < 4; i++) {
            float p = Math.max(0f, Math.min(1f, progress - i * 0.18f));
            if (p <= 0f) continue;
            paint.setAlpha((int)((1f - p) * 220));
            paint.setStrokeWidth(7f * (1f - p * 0.7f));
            c.drawCircle(cx, cy, maxR * p, paint);
        }
        if (progress < 1f) postInvalidateOnAnimation();
    }
}
