package com.music.app.util;
import android.os.Build;
import android.view.Choreographer;
public class FrameController implements Choreographer.FrameCallback {
    public interface Tick { void onFrame(); }
    private Choreographer chor;
    private Tick tick;
    private int fps = 60;
    private boolean running = false;
    private long last = 0;
    public FrameController(int fps, Tick t) {
        this.tick = t;
        if (fps < 30) fps = 30;
        if (fps > 120) fps = 120;
        this.fps = fps;
        if (Build.VERSION.SDK_INT >= 16) chor = Choreographer.getInstance();
    }
    public void start() {
        if (running || chor == null) return;
        running = true;
        last = 0;
        chor.postFrameCallback(this);
    }
    public void stop() {
        running = false;
        if (chor != null) chor.removeFrameCallback(this);
    }
    @Override public void doFrame(long ns) {
        if (!running) return;
        if (last == 0) last = ns;
        long dt = ns - last;
        long interval = 1_000_000_000L / fps;
        if (dt >= interval) {
            last = ns;
            if (tick != null) tick.onFrame();
        }
        if (chor != null) chor.postFrameCallback(this);
    }
}
