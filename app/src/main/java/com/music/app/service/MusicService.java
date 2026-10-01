package com.music.app.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.music.app.LockScreenActivity;

public class MusicService extends MediaSessionService {
    private MediaSession session;
    public static ExoPlayer sharedPlayer;

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
        } catch (Throwable ignored) {}
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        // 不释放 sharedPlayer，让它跟随 App 生命周期
        super.onDestroy();
    }
}
