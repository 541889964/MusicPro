package com.music.app.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent it) {
        try {
            if (it == null) return;
            String a = it.getAction();
            if (Intent.ACTION_BOOT_COMPLETED.equals(a)
                || "android.intent.action.QUICKBOOT_POWERON".equals(a)) {
                // 开机自启灵动岛（不播放音乐）
                if (Build.VERSION.SDK_INT >= 23
                    && !android.provider.Settings.canDrawOverlays(ctx)) return;
                Intent svc = new Intent(ctx, IslandService.class);
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(svc);
                else ctx.startService(svc);
            }
        } catch (Throwable ignored) {}
    }
}
