package com.music.app.service;

import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

public class MusicService extends MediaSessionService {
    private MediaSession session;
    public static ExoPlayer sharedPlayer;

    @Override public void onCreate() {
        super.onCreate();
        try {
            if (sharedPlayer == null) sharedPlayer = new ExoPlayer.Builder(this).build();
            session = new MediaSession.Builder(this, sharedPlayer).build();
        } catch (Throwable ignored) {}
    }

    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo c) { return session; }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }
}
