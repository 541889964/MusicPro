package com.music.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;
import com.music.app.PlayerActivity;
import com.music.app.R;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaSessionService {
    private static final String CH_ID = "music_playback";
    private MediaSession session;
    private static ExoPlayer sharedPlayer;
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
                    @Override public void onIsPlayingChanged(boolean playing) {
                        pushNotification(playing);
                    }
                    @Override public void onMediaItemTransition(MediaItem item, int r) {
                        if (item != null) {
                            sharedIndex = sharedPlayer.getCurrentMediaItemIndex();
                            pushNotification(sharedPlayer.isPlaying());
                        }
                    }
                    @Override public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_ENDED) {
                            if (sharedIndex < sharedQueue.size() - 1) {
                                sharedIndex++;
                                seekTo(sharedIndex, true);
                            } else {
                                sharedIndex = 0;
                                seekTo(0, true);
                            }
                        }
                    }
                });
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, PlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                flags);
            session = new MediaSession.Builder(this, sharedPlayer)
                .setSessionActivity(pi).build();
            startForegroundSafe(false);
        } catch (Throwable ignored) {}
    }

    private void startForegroundSafe(boolean playing) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(CH_ID) == null) {
                    nm.createNotificationChannel(new NotificationChannel(
                        CH_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW));
                }
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0,
                new Intent(this, PlayerActivity.class)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
                flags);
            String title = "MUSIC·Pro", text = playing ? "正在播放" : "已暂停";
            try {
                MediaItem it = sharedPlayer != null ? sharedPlayer.getCurrentMediaItem() : null;
                if (it != null && it.mediaMetadata != null) {
                    if (it.mediaMetadata.title != null) title = it.mediaMetadata.title.toString();
                    if (it.mediaMetadata.artist != null) text = it.mediaMetadata.artist.toString();
                }
            } catch (Throwable ignored) {}
            Notification n = new NotificationCompat.Builder(this, CH_ID)
                .setContentTitle(title)
                .setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentIntent(pi)
                .setOngoing(playing)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
            startForeground(1, n);
        } catch (Throwable ignored) {}
    }

    private void pushNotification(boolean playing) {
        try { startForegroundSafe(playing); } catch (Throwable ignored) {}
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    // ============ 静态 API ============
    public static ExoPlayer getPlayer() { return sharedPlayer; }

    public static void setQueue(Context ctx, List<Song> q, int idx) {
        sharedQueue = new ArrayList<Song>(q);
        sharedIndex = idx;
        rebuild(ctx, idx, true);
    }

    public static void rebuild(Context ctx, int startIdx, boolean play) {
        if (sharedPlayer == null) return;
        if (sharedQueue == null || sharedQueue.isEmpty()) return;
        try {
            List<MediaItem> items = new ArrayList<MediaItem>();
            for (int i = 0; i < sharedQueue.size(); i++) {
                Song sg = sharedQueue.get(i);
                String uri;
                if (sg.isOnline) {
                    uri = sg.onlineUrl;
                    if (uri == null || uri.isEmpty())
                        uri = "https://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
                } else {
                    uri = "file://" + sg.path;
                }
                items.add(new MediaItem.Builder().setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(sg.title).setArtist(sg.artist).build())
                    .build());
            }
            sharedPlayer.setMediaItems(items, startIdx, 0);
            sharedPlayer.prepare();
            if (play) sharedPlayer.play();
        } catch (Throwable ignored) {}
    }

    public static void play(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true);
            return;
        }
        sharedPlayer.play();
    }
    public static void pause(Context ctx) {
        ensure(ctx);
        if (sharedPlayer != null) sharedPlayer.pause();
    }
    public static void toggle(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true);
            return;
        }
        if (sharedPlayer.isPlaying()) sharedPlayer.pause();
        else sharedPlayer.play();
    }
    public static void next(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true); return;
        }
        int total = sharedPlayer.getMediaItemCount();
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        int ni = idx < total - 1 ? idx + 1 : 0;
        sharedIndex = ni;
        sharedPlayer.seekTo(ni, 0);
        sharedPlayer.play();
    }
    public static void prev(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true); return;
        }
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        int pi = idx > 0 ? idx - 1 : 0;
        sharedIndex = pi;
        sharedPlayer.seekTo(pi, 0);
        sharedPlayer.play();
    }
    public static void seekTo(int idx, boolean play) {
        if (sharedPlayer == null) return;
        sharedPlayer.seekTo(idx, 0);
        if (play) sharedPlayer.play();
    }
    public static void seekToPos(long ms) {
        if (sharedPlayer != null) sharedPlayer.seekTo(ms);
    }
    public static boolean isPlaying() {
        return sharedPlayer != null && sharedPlayer.isPlaying();
    }
    public static long getPos() {
        return sharedPlayer != null ? sharedPlayer.getCurrentPosition() : 0;
    }
    public static long getDur() {
        return sharedPlayer != null ? sharedPlayer.getDuration() : 0;
    }
    public static int getIndex() {
        return sharedPlayer != null ? sharedPlayer.getCurrentMediaItemIndex() : 0;
    }
    public static MediaItem getCurrentItem() {
        return sharedPlayer != null ? sharedPlayer.getCurrentMediaItem() : null;
    }

    private static void ensure(Context ctx) {
        if (!started && ctx != null) {
            try {
                ctx.startService(new Intent(ctx, MusicService.class));
                started = true;
            } catch (Throwable ignored) {}
        }
    }

    @Override public void onDestroy() {
        // 不释放 player，让它继续
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }
}
