package com.music.app.widget;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class FloatingHeartsView extends View {
    static class H { float x, y, vy, size, alpha, rot, rotV; int color; }
    final List<H> hearts = new ArrayList<H>();
    final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    final Random rnd = new Random();
    boolean running = false;
    final Path path = new Path();
    final int[] COLORS = {0xFFFF6B9D,0xFFFFB6D0,0xFF9B6BFF,0xFFB8A8E8,0xFFFF8FB1};
    public FloatingHeartsView(Context c) { super(c); init(); }
    public FloatingHeartsView(Context c, AttributeSet a) { super(c, a); init(); }
    void init() { for (int i = 0; i < 24; i++) hearts.add(spawn(true)); }
    H spawn(boolean initial) {
        H h = new H();
        int w = getWidth() > 0 ? getWidth() : 1080;
        int hh = getHeight() > 0 ? getHeight() : 1920;
        h.x = rnd.nextFloat() * w;
        h.y = initial ? rnd.nextFloat() * hh : hh + 60;
        h.vy = 0.7f + rnd.nextFloat() * 1.8f;
        h.size = 8f + rnd.nextFloat() * 22f;
        h.alpha = 0.15f + rnd.nextFloat() * 0.45f;
        h.rot = rnd.nextFloat() * 360;
        h.rotV = (rnd.nextFloat() - 0.5f) * 1.8f;
        h.color = COLORS[rnd.nextInt(COLORS.length)];
        return h;
    }
    public void start(int ms) {
        running = true;
        ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.setDuration(ms);
        a.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator an) {
                if (!running) return;
                for (int i = 0; i < hearts.size(); i++) {
                    H h = hearts.get(i);
                    h.y -= h.vy; h.rot += h.rotV;
                    if (h.y < -60) {
                        H n = spawn(false);
                        h.x = n.x; h.y = n.y; h.size = n.size;
                        h.alpha = n.alpha; h.rot = n.rot; h.color = n.color;
                    }
                }
                postInvalidateOnAnimation();
            }
        });
        a.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) { running = false; postInvalidateOnAnimation(); }
        });
        a.start();
    }
    @Override protected void onDraw(Canvas canvas) {
        if (!running) return;
        for (int i = 0; i < hearts.size(); i++) {
            H h = hearts.get(i);
            paint.setAlpha((int)(h.alpha * 255));
            paint.setColor(h.color);
            canvas.save();
            canvas.translate(h.x, h.y);
            canvas.rotate(h.rot);
            drawHeart(canvas, h.size);
            canvas.restore();
        }
    }
    void drawHeart(Canvas c, float s) {
        path.reset();
        path.moveTo(0, -s * 0.3f);
        path.cubicTo(-s * 0.5f, -s * 0.9f, -s, -s * 0.2f, 0, s * 0.6f);
        path.cubicTo(s, -s * 0.2f, s * 0.5f, -s * 0.9f, 0, -s * 0.3f);
        c.drawPath(path, paint);
    }
}
