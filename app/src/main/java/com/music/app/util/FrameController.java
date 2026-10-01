package com.music.app.util;

import android.content.Context;
import android.os.Build;
import android.view.Choreographer;

/**
 * 帧率控制器：控制灵动岛动画刷新率
 * - 支持 30 / 60 / 90 / 120 / 144 fps
 * - 不支持的屏幕用 Choreographer 跳帧策略，让动画看起来高刷
 */
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
        this.targetFps = fps;
        this.frameInterval = 1000000000L / fps;
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

        // ★ 帧率跳帧策略：
        // 屏幕刷新率是固定的（如 60Hz），我们通过降低回调频率实现 30fps
        // 但如果要 90/120fps 而屏幕只有 60Hz，就每帧都回调 + 让动画跑"两次"到达指定视觉速度
        if (delta >= frameInterval) {
            lastFrameTime = nanos;
            if (tick != null) tick.onFrame(delta);
            // 高刷补偿：目标 > 屏幕实际，就多跑一次让动画更"密"
            if (targetFps > 60) {
                if (tick != null) tick.onFrame(delta / 2);
            }
        }
        if (chor != null) chor.postFrameCallback(this);
    }
}
