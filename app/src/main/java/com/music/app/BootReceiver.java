package com.music.app;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent it) {
        if (it == null) return;
        String a = it.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(a)
            || "android.intent.action.QUICKBOOT_POWERON".equals(a)) {
            try {
                if (Build.VERSION.SDK_INT >= 23
                    && android.provider.Settings.canDrawOverlays(ctx)) {
                    Intent s = new Intent(ctx, com.music.app.service.IslandService.class);
                    if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(s);
                    else ctx.startService(s);
                }
                Intent g = new Intent(ctx, com.music.app.service.GuardService.class);
                if (Build.VERSION.SDK_INT >= 26) ctx.startForegroundService(g);
                else ctx.startService(g);
            } catch (Throwable ignored) {}
        }
    }
}
