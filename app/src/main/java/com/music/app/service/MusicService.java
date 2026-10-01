package com.music.app.service;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.music.app.PlayerActivity;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaSessionService {
    private MediaSession session;
    public static ExoPlayer sharedPlayer;
    public static List<Song> sharedQueue = new ArrayList<Song>();
    public static int sharedIndex = 0;
    public static boolean started = false;

    @Override public void onCreate() {
        super.onCreate();
        started = true;
        try {
            if (sharedPlayer == null) {
                sharedPlayer = new ExoPlayer.Builder(this).build();
                sharedPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                sharedPlayer.addListener(new Player.Listener() {
                    @Override public void onMediaItemTransition(MediaItem item, int r) {
                        if (item != null) sharedIndex = sharedPlayer.getCurrentMediaItemIndex();
                    }
                    @Override public void onPlaybackStateChanged(int s) {
                        if (s == Player.STATE_ENDED && !sharedQueue.isEmpty()) next(getApplicationContext());
                    }
                });
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, PlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP), flags);
            session = new MediaSession.Builder(this, sharedPlayer).setSessionActivity(pi).build();
        } catch (Throwable ignored) {}
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    public static ExoPlayer getPlayer() { return sharedPlayer; }
    public static boolean isPlaying() { return sharedPlayer != null && sharedPlayer.isPlaying(); }
    public static long getPos() { return sharedPlayer != null ? sharedPlayer.getCurrentPosition() : 0; }
    public static long getDur() { return sharedPlayer != null ? sharedPlayer.getDuration() : 0; }
    public static int getIndex() { return sharedPlayer != null ? sharedPlayer.getCurrentMediaItemIndex() : 0; }
    public static MediaItem getCurrentItem() { return sharedPlayer != null ? sharedPlayer.getCurrentMediaItem() : null; }

    public static synchronized ExoPlayer ensurePlayer(Context ctx) {
        if (sharedPlayer == null && ctx != null) {
            try {
                sharedPlayer = new ExoPlayer.Builder(ctx).build();
                sharedPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            } catch (Throwable ignored) {}
        }
        return sharedPlayer;
    }

    public static void playItems(List<MediaItem> items, int startIdx) {
        if (sharedPlayer == null || items == null || items.isEmpty()) return;
        if (startIdx < 0 || startIdx >= items.size()) startIdx = 0;
        try {
            sharedPlayer.setMediaItems(items, startIdx, 0);
            sharedPlayer.prepare();
            sharedPlayer.play();
        } catch (Throwable ignored) {}
    }

    public static void toggle(Context c) {
        ExoPlayer p = getPlayer();
        if (p == null) return;
        if (p.isPlaying()) p.pause(); else p.play();
    }
    public static void next(Context c) {
        ExoPlayer p = getPlayer();
        if (p == null) return;
        try {
            int total = p.getMediaItemCount();
            int idx = p.getCurrentMediaItemIndex();
            if (total <= 1) { p.seekTo(0, 0); p.play(); return; }
            p.seekTo(idx < total - 1 ? idx + 1 : 0, 0);
            p.play();
        } catch (Throwable ignored) {}
    }
    public static void prev(Context c) {
        ExoPlayer p = getPlayer();
        if (p == null) return;
        try {
            int idx = p.getCurrentMediaItemIndex();
            p.seekTo(idx > 0 ? idx - 1 : 0, 0);
            p.play();
        } catch (Throwable ignored) {}
    }
    public static void seekToPos(long ms) {
        ExoPlayer p = getPlayer();
        if (p != null) p.seekTo(ms);
    }

    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }
}
