package com.music.app.service;

import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.music.app.LockScreenActivity;
import com.music.app.PlayerActivity;

public class MusicService extends MediaSessionService {
    private MediaSession session;
    public static ExoPlayer sharedPlayer;

    @Override public void onCreate() {
        super.onCreate();
        try {
            sharedPlayer = new ExoPlayer.Builder(this).build();
            session = new MediaSession.Builder(this, sharedPlayer)
                .setSessionActivity(buildLockPending())
                .build();
        } catch (Throwable ignored) {}
    }

    private PendingIntent buildLockPending() {
        Intent it = new Intent(this, LockScreenActivity.class);
        it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        return PendingIntent.getActivity(this, 0, it, flags);
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        if (sharedPlayer != null) { sharedPlayer.release(); sharedPlayer = null; }
        super.onDestroy();
    }
}
