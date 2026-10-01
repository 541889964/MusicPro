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
    @Override public void onCreate() {
        super.onCreate();
        try {
            if (sharedPlayer == null) {
                sharedPlayer = new ExoPlayer.Builder(this).build();
                sharedPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, PlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                flags);
            session = new MediaSession.Builder(this, sharedPlayer).setSessionActivity(pi).build();
        } catch (Throwable ignored) {}
    }
    @Nullable @Override public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }
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
    public static void setQueue(Context ctx, List<Song> q, int idx) {
        sharedQueue = new ArrayList<Song>(q);
        sharedIndex = idx;
        if (sharedPlayer != null) rebuild(ctx, idx, true);
    }
    public static void rebuild(Context ctx, int startIdx, boolean play) {
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        try {
            List<MediaItem> items = new ArrayList<MediaItem>();
            for (int i = 0; i < sharedQueue.size(); i++) {
                Song sg = sharedQueue.get(i);
                String uri = uriOf(sg);
                items.add(new MediaItem.Builder().setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(sg.title).setArtist(sg.artist).build()).build());
            }
            sharedPlayer.setMediaItems(items, startIdx, 0);
            sharedPlayer.prepare();
            if (play) sharedPlayer.play();
        } catch (Throwable ignored) {}
    }
    private static String uriOf(Song sg) {
        if (sg.isOnline) {
            String u = sg.onlineUrl;
            if (u == null || u.isEmpty())
                u = "http://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
            return u;
        }
        return "file://" + sg.path;
    }
    public static void play(Context ctx) {
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true); return;
        }
        sharedPlayer.play();
    }
    public static void pause(Context ctx) { if (sharedPlayer != null) sharedPlayer.pause(); }
    public static void toggle(Context ctx) {
        if (sharedPlayer == null) return;
        if (sharedPlayer.isPlaying()) sharedPlayer.pause(); else sharedPlayer.play();
    }
    public static void next(Context ctx) {
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        int total = sharedPlayer.getMediaItemCount();
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        if (total <= 1) { sharedPlayer.seekTo(0, 0); sharedPlayer.play(); return; }
        sharedPlayer.seekTo(idx < total - 1 ? idx + 1 : 0, 0);
        sharedPlayer.play();
    }
    public static void prev(Context ctx) {
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        sharedPlayer.seekTo(idx > 0 ? idx - 1 : 0, 0);
        sharedPlayer.play();
    }
    public static void seekToPos(long ms) { if (sharedPlayer != null) sharedPlayer.seekTo(ms); }
}
