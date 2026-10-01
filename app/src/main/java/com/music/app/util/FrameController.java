package com.music.app.util;

import android.os.Build;
import android.view.Choreographer;

public class FrameController implements Choreographer.FrameCallback {
    public interface Tick { void onFrame(long dtNanos); }
    private static Choreographer chor;
    private final Tick tick;
    private volatile boolean running = false;
    private volatile int targetFps = 60;
    private long lastFrameTime = 0;
    private long frameInterval = 0;

    public FrameController(int fps, Tick t) {
        this.tick = t;
        setFps(fps);
        if (Build.VERSION.SDK_INT >= 16) chor = Choreographer.getInstance();
    }
    public void setFps(int fps) {
        if (fps < 30) fps = 30;
        if (fps > 240) fps = 240;
        targetFps = fps;
        frameInterval = 1000000000L / fps;
    }
    public int getFps() { return targetFps; }
    public void start() {
        if (running) return;
        running = true;
        lastFrameTime = 0;
        if (chor != null) chor.postFrameCallback(this);
    }
    public void stop() {
        running = false;
        if (chor != null) chor.removeFrameCallback(this);
    }
    @Override public void doFrame(long nanos) {
        if (!running) return;
        if (lastFrameTime == 0) lastFrameTime = nanos;
        long delta = nanos - lastFrameTime;
        if (delta >= frameInterval) {
            lastFrameTime = nanos;
            if (tick != null) tick.onFrame(delta);
            if (targetFps > 60 && tick != null) tick.onFrame(delta / 2);
        }
        if (chor != null) chor.postFrameCallback(this);
    }
}
