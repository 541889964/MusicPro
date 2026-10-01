package com.music.app.service;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
public class MusicService extends MediaSessionService {
    private MediaSession session;
    @Override public void onCreate() {
        super.onCreate();
        session = new MediaSession.Builder(this,
            new ExoPlayer.Builder(this).build()).build();
    }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo ci) {
        return session;
    }
    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }
}
