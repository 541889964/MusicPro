package com.music.app.util;

import android.os.Build;
import android.view.Choreographer;

public class FrameController implements Choreographer.FrameCallback {
    public interface Tick { void onFrame(); }

    private final Choreographer chor;
    private final Tick tick;
    private volatile int targetFps = 60;
    private volatile boolean running = false;
    private long lastFrame = 0;

    public FrameController(int fps, Tick t) {
        this.tick = t;
        setFps(fps);
        this.chor = Build.VERSION.SDK_INT >= 16
            ? Choreographer.getInstance()
            : null;
    }

    public void setFps(int fps) {
        if (fps < 30) fps = 30;
        if (fps > 144) fps = 144;
        this.targetFps = fps;
    }

    public void start() {
        if (running || chor == null) return;
        running = true;
        lastFrame = 0;
        chor.postFrameCallback(this);
    }

    public void stop() {
        running = false;
        if (chor != null) chor.removeFrameCallback(this);
    }

    @Override public void doFrame(long nanos) {
        if (!running) return;
        if (lastFrame == 0) lastFrame = nanos;
        long dt = nanos - lastFrame;
        long interval = 1_000_000_000L / targetFps;
        // 补帧：屏幕实际刷新率低于目标时，每帧都跑让动画更密
        if (dt >= interval) {
            lastFrame = nanos;
            if (tick != null) tick.onFrame();
        }
        if (chor != null) chor.postFrameCallback(this);
    }
}
