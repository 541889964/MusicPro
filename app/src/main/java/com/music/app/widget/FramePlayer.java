package com.music.app.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import java.io.InputStream;

/** 播放 assets/frames/ 下的 PNG 序列帧 */
public class FramePlayer {
    private static final String TAG = "FramePlayer";

    public interface OnEnd { void onEnd(); }

    private final Context ctx;
    private final ImageView target;
    private final Handler h = new Handler(Looper.getMainLooper());
    private String prefix;
    private int totalFrames;
    private int fps;
    private int currentFrame;
    private boolean playing;
    private OnEnd onEnd;

    public FramePlayer(Context ctx, ImageView target) {
        this.ctx = ctx;
        this.target = target;
    }

    public void play(String prefix, int totalFrames, int fps, OnEnd onEnd) {
        stop();
        this.prefix = prefix;
        this.totalFrames = totalFrames;
        this.fps = fps;
        this.onEnd = onEnd;
        this.currentFrame = 0;
        this.playing = true;
        try { target.setVisibility(ImageView.VISIBLE); } catch (Throwable ignored) {}
        h.postDelayed(tick, 1000 / fps);
    }

    public void stop() {
        playing = false;
        h.removeCallbacks(tick);
        try { target.setVisibility(ImageView.GONE); } catch (Throwable ignored) {}
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!playing) return;
            try {
                String name = String.format("frames/%s_%03d.png", prefix, currentFrame);
                InputStream is = ctx.getAssets().open(name);
                Bitmap bm = BitmapFactory.decodeStream(is);
                is.close();
                if (bm != null) target.setImageBitmap(bm);
            } catch (Throwable t) {
                Log.e(TAG, "frame " + currentFrame, t);
            }
            currentFrame++;
            if (currentFrame >= totalFrames) {
                playing = false;
                if (onEnd != null) onEnd.onEnd();
                return;
            }
            h.postDelayed(this, 1000 / fps);
        }
    };
}
