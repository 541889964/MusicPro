package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
public class BreathingGlowView extends View {
    private float phase = 0f;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    public BreathingGlowView(Context c){ super(c); init(); }
    public BreathingGlowView(Context c, AttributeSet a){ super(c,a); init(); }
    private void init(){
        p.setColor(Color.parseColor("#FF6B9D"));
        ValueAnimator a = ValueAnimator.ofFloat(0f, 1f);
        a.setDuration(3000); a.setRepeatCount(ValueAnimator.INFINITE);
        a.setInterpolator(new AccelerateDecelerateInterpolator());
        a.addUpdateListener(an -> { phase = (float) an.getAnimatedValue(); invalidate(); });
        a.start();
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        float cx = getWidth()/2f, cy = getHeight()/2f;
        float maxR = Math.min(cx, cy) * 0.9f;
        float baseR = maxR * (0.85f + 0.15f * phase);
        for (int i = 5; i >= 1; i--) {
            p.setAlpha((int)(15 + 25 * (1 - phase) * (1 - i / 6f)));
            c.drawCircle(cx, cy, baseR * i / 5f, p);
        }
    }
}
