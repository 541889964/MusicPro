package com.music.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

public class ScreenReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent it) {
        if (it == null) return;
        String action = it.getAction();
        if (Intent.ACTION_SCREEN_OFF.equals(action)) {
            if (PlayerActivity.player != null
                && PlayerActivity.player.isPlaying()
                && PlayerActivity.queue != null
                && !PlayerActivity.queue.isEmpty()) {
                // 延迟 300ms 等系统锁屏完成
                new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        try {
                            LockScreenActivity.queue = PlayerActivity.queue;
                            LockScreenActivity.currentIndex = PlayerActivity.currentIndex;
                            Intent i = new Intent(ctx, LockScreenActivity.class);
                            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                                | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            ctx.startActivity(i);
                        } catch (Throwable ignored) {}
                    }
                }, 350);
            }
        }
    }
}
