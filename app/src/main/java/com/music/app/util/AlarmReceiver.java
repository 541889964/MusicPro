package com.music.app.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.music.app.MainActivity;
import com.music.app.R;

public class AlarmReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context ctx, Intent it) {
        try {
            // 播放系统铃声
            Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (uri == null) uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            final MediaPlayer mp = MediaPlayer.create(ctx, uri);
            if (mp != null) {
                mp.setLooping(false);
                mp.start();
                // 30 秒后自动停
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(new Runnable() {
                    @Override public void run() {
                        try { if (mp.isPlaying()) mp.stop(); mp.release(); } catch (Throwable ignored) {}
                    }
                }, 30000);
            }

            // 发通知
            String chId = "alarm";
            NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                if (nm.getNotificationChannel(chId) == null) {
                    nm.createNotificationChannel(new NotificationChannel(
                        chId, "闹钟", NotificationManager.IMPORTANCE_HIGH));
                }
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, MainActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP), flags);

            Notification n = new NotificationCompat.Builder(ctx, chId)
                .setContentTitle("⏰ 闹钟响了")
                .setContentText("该起床/做事啦")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
            if (nm != null) nm.notify(200, n);

            // 也让灵动岛显示
            try {
                Class<?> cls = Class.forName("com.music.app.service.IslandService");
                cls.getMethod("notify", String.class, String.class)
                    .invoke(null, "⏰ 闹钟响了", "该起床/做事啦");
            } catch (Throwable ignored) {}

            // 重新设置明天的闹钟
            AlarmHelper.set(ctx, AlarmHelper.getHour(ctx), AlarmHelper.getMin(ctx));
        } catch (Throwable ignored) {}
    }
}
