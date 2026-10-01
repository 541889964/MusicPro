package com.music.app.service;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.Nullable;
public class GuardService extends Service {
    private static final String CH = "guard";
    private final Handler h = new Handler(Looper.getMainLooper());
    @Nullable @Override public IBinder onBind(Intent i) { return null; }
    @Override public void onCreate() {
        super.onCreate();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(CH) == null) {
                    NotificationChannel c = new NotificationChannel(CH, "后台守护", NotificationManager.IMPORTANCE_MIN);
                    nm.createNotificationChannel(c);
                }
                Notification n = new Notification.Builder(this, CH)
                    .setSmallIcon(com.music.app.R.mipmap.ic_launcher)
                    .setContentTitle("拾音").setContentText("守护中").setOngoing(true).build();
                startForeground(2, n);
            }
            h.post(beat);
        } catch (Throwable ignored) {}
    }
    private final Runnable beat = new Runnable() {
        @Override public void run() {
            try {
                if (IslandService.instance == null
                    && Build.VERSION.SDK_INT >= 23
                    && android.provider.Settings.canDrawOverlays(GuardService.this)) {
                    Intent i = new Intent(GuardService.this, IslandService.class);
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                    else startService(i);
                }
                if (MusicService.getPlayer() == null) {
                    Intent i = new Intent(GuardService.this, MusicService.class);
                    if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
                    else startService(i);
                }
            } catch (Throwable ignored) {}
            h.postDelayed(this, 10000);
        }
    };
    @Override public int onStartCommand(Intent intent, int flags, int startId) { return START_STICKY; }
    @Override public void onTaskRemoved(Intent rootIntent) {
        try {
            Intent r = new Intent(getApplicationContext(), GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(r);
            else startService(r);
            if (Build.VERSION.SDK_INT >= 23
                && android.provider.Settings.canDrawOverlays(this)) {
                Intent isl = new Intent(this, IslandService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(isl);
                else startService(isl);
            }
        } catch (Throwable ignored) {}
        super.onTaskRemoved(rootIntent);
    }
    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        try {
            Intent i = new Intent(this, GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i);
            else startService(i);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
