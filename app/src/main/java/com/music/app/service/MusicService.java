package com.music.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.music.app.LockScreenActivity;
import com.music.app.R;
import com.music.app.model.Song;
import java.util.List;

public class MusicService extends MediaSessionService {
    private MediaSession session;
    public static ExoPlayer sharedPlayer;
    public static List<Song> sharedQueue;
    public static int sharedIndex = 0;

    @Override public void onCreate() {
        super.onCreate();
        try {
            if (sharedPlayer == null) {
                sharedPlayer = new ExoPlayer.Builder(this).build();
                sharedPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            }
            Intent it = new Intent(this, LockScreenActivity.class);
            it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0, it, flags);
            session = new MediaSession.Builder(this, sharedPlayer)
                .setSessionActivity(pi).build();
            startForegroundSafely();
        } catch (Throwable ignored) {}
    }

    private void startForegroundSafely() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                String chId = "music_playback";
                NotificationChannel ch = new NotificationChannel(
                    chId, "音乐播放", NotificationManager.IMPORTANCE_LOW);
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null) nm.createNotificationChannel(ch);
            }
            Intent ni = new Intent(this, LockScreenActivity.class);
            ni.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            int iflags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) iflags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0, ni, iflags);
            Notification n = new NotificationCompat.Builder(this, "music_playback")
                .setContentTitle("MUSIC·Pro")
                .setContentText("正在后台播放")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            startForeground(1, n);
        } catch (Throwable ignored) {}
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        // ★ 不释放 sharedPlayer，保持后台播放
        super.onDestroy();
    }
}
