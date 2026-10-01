package com.music.app.widget;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import java.util.*;
public class FloatingHeartsView extends View {
    private static class H { float x,y,vy,size,alpha,rot,rotV; int color; }
    private final List<H> hearts = new ArrayList<>();
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random rnd = new Random();
    private boolean running = false;
    private final Path path = new Path();
    private final int[] COLORS = {0xFFFF6B9D, 0xFFFFB6D0, 0xFF9B6BFF, 0xFFB8A8E8, 0xFFFF8FB1};

    public FloatingHeartsView(Context c){ super(c); init(); }
    public FloatingHeartsView(Context c, AttributeSet a){ super(c,a); init(); }

    private void init(){
        for (int i = 0; i < 26; i++) hearts.add(spawn(true));
    }
    private H spawn(boolean init) {
        H h = new H();
        h.x = rnd.nextFloat() * (getWidth() > 0 ? getWidth() : 1080);
        h.y = init ? rnd.nextFloat() * (getHeight() > 0 ? getHeight() : 1920)
                   : (getHeight() > 0 ? getHeight() : 1920) + 60;
        h.vy = 0.7f + rnd.nextFloat() * 1.8f;
        h.size = 8f + rnd.nextFloat() * 22f;
        h.alpha = 0.15f + rnd.nextFloat() * 0.45f;
        h.rot = rnd.nextFloat() * 360;
        h.rotV = (rnd.nextFloat() - 0.5f) * 1.8f;
        h.color = COLORS[rnd.nextInt(COLORS.length)];
        return h;
    }
    public void start(int durationMs) {
        running = true;
        ValueAnimator a = ValueAnimator.ofFloat(0, 1);
        a.setDuration(durationMs);
        a.addUpdateListener(an -> {
            if (!running) return;
            for (H h : hearts) {
                h.y -= h.vy; h.rot += h.rotV;
                if (h.y < -60) {
                    H n = spawn(false);
                    h.x = n.x; h.y = n.y; h.size = n.size;
                    h.alpha = n.alpha; h.rot = n.rot; h.color = n.color;
                }
            }
            invalidate();
        });
        a.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) { running = false; invalidate(); }
        });
        a.start();
    }
    @Override protected void onDraw(Canvas canvas) {
        if (!running) return;
        for (H h : hearts) {
            p.setAlpha((int)(h.alpha * 255));
            p.setColor(h.color);
            canvas.save();
            canvas.translate(h.x, h.y);
            canvas.rotate(h.rot);
            drawHeart(canvas, h.size);
            canvas.restore();
        }
        postInvalidateOnAnimation();
    }
    private void drawHeart(Canvas c, float s) {
        path.reset();
        path.moveTo(0, -s * 0.3f);
        path.cubicTo(-s * 0.5f, -s * 0.9f, -s, -s * 0.2f, 0, s * 0.6f);
        path.cubicTo(s, -s * 0.2f, s * 0.5f, -s * 0.9f, 0, -s * 0.3f);
        c.drawPath(path, p);
    }
}
