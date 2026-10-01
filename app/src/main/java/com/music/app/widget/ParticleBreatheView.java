package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.Random;

public class ParticleBreatheView extends View {
    private static class Particle {
        float x, y, vy, r, alpha;
    }
    private Particle[] parts = new Particle[30];
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random(42);
    private ValueAnimator va;
    private boolean running = false;

    public ParticleBreatheView(Context c) { super(c); init(); }
    public ParticleBreatheView(Context c, AttributeSet a) { super(c, a); init(); }

    private void init() {
        p.setColor(0xFFFFB6D0);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new Particle();
            parts[i].x = rnd.nextFloat();
            parts[i].y = rnd.nextFloat();
            parts[i].vy = 0.001f + rnd.nextFloat() * 0.003f;
            parts[i].r = 1.5f + rnd.nextFloat() * 2.5f;
            parts[i].alpha = 0.3f + rnd.nextFloat() * 0.5f;
        }
    }

    public void start() {
        if (running) return;
        running = true;
        va = ValueAnimator.ofFloat(0, 1);
        va.setDuration(3000);
        va.setRepeatCount(ValueAnimator.INFINITE);
        va.addUpdateListener(a -> {
            for (Particle pt : parts) {
                pt.y -= pt.vy;
                if (pt.y < -0.05f) {
                    pt.y = 1.05f;
                    pt.x = rnd.nextFloat();
                }
            }
            postInvalidateOnAnimation();
        });
        va.start();
    }

    public void stop() {
        running = false;
        if (va != null) va.cancel();
        postInvalidateOnAnimation();
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (!running) return;
        float w = getWidth(), h = getHeight();
        for (Particle pt : parts) {
            p.setAlpha((int)(pt.alpha * 255));
            c.drawCircle(pt.x * w, pt.y * h, pt.r, p);
        }
    }
}
