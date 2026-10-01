package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;

public class ParticleView extends View {
    private static class P { float x,y,vx,vy,r,alpha,hue; }
    private final List<P> parts = new ArrayList<>();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private boolean running = false;

    public ParticleView(Context c){ super(c); init(); }
    public ParticleView(Context c, AttributeSet a){ super(c,a); init(); }

    private void init(){
        paint.setStyle(Paint.Style.FILL);
        for (int i = 0; i < 60; i++) {
            P p = new P();
            p.x = rnd.nextFloat();
            p.y = rnd.nextFloat();
            p.vx = (rnd.nextFloat() - 0.5f) * 0.0015f;
            p.vy = (rnd.nextFloat() - 0.5f) * 0.0015f;
            p.r = 1f + rnd.nextFloat() * 3.5f;
            p.alpha = 0.2f + rnd.nextFloat() * 0.6f;
            p.hue = 260 + rnd.nextFloat() * 60;
            parts.add(p);
        }
    }

    public void start(int durationMs) {
        running = true;
        ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.setDuration(durationMs);
        a.addUpdateListener(an -> {
            if (!running) return;
            for (P p : parts) {
                p.x += p.vx; p.y += p.vy;
                if (p.x < 0) p.x = 1; if (p.x > 1) p.x = 0;
                if (p.y < 0) p.y = 1; if (p.y > 1) p.y = 0;
            }
            invalidate();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                running = false;
            }
        });
        a.start();
    }

    @Override protected void onDraw(Canvas canvas) {
        if (!running) return;
        int w = getWidth(), h = getHeight();
        for (P p : parts) {
            paint.setAlpha((int)(p.alpha * 255));
            paint.setColor(Color.HSVToColor(new float[]{ p.hue, 0.6f, 1f}));
            canvas.drawCircle(p.x * w, p.y * h, p.r, paint);
        }
        postInvalidateOnAnimation();
    }
}
