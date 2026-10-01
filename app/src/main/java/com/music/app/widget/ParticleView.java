package com.music.app.widget;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class ParticleView extends View {
    static class P { float x, y, vx, vy, r, alpha, hue; }
    final List<P> parts = new ArrayList<P>();
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random rnd = new Random();
    boolean running = false;
    public ParticleView(Context c) { super(c); init(); }
    public ParticleView(Context c, AttributeSet a) { super(c, a); init(); }
    void init() {
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
    public void start(int ms) {
        running = true;
        ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.setDuration(ms);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator an) {
                if (!running) return;
                for (int i = 0; i < parts.size(); i++) {
                    P p = parts.get(i);
                    p.x += p.vx; p.y += p.vy;
                    if (p.x < 0) p.x = 1;
                    if (p.x > 1) p.x = 0;
                    if (p.y < 0) p.y = 1;
                    if (p.y > 1) p.y = 0;
                }
                postInvalidateOnAnimation();
            }
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { running = false; }
        });
        a.start();
    }
    @Override protected void onDraw(Canvas canvas) {
        if (!running) return;
        int w = getWidth(), h = getHeight();
        for (int i = 0; i < parts.size(); i++) {
            P p = parts.get(i);
            paint.setAlpha((int)(p.alpha * 255));
            paint.setColor(Color.HSVToColor(new float[]{p.hue, 0.6f, 1f}));
            canvas.drawCircle(p.x * w, p.y * h, p.r, paint);
        }
    }
}
