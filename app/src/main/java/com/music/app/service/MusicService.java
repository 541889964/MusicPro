package com.music.app.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
import com.music.app.R;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;

public class MusicService extends MediaSessionService {
    private static final String CH_ID = "music_playback";
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
                        if (item != null) {
                            sharedIndex = sharedPlayer.getCurrentMediaItemIndex();
                            updateNotif();
                        }
                    }
                    @Override public void onIsPlayingChanged(boolean p) { updateNotif(); }
                    @Override public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_ENDED && !sharedQueue.isEmpty()) {
                            next(getApplicationContext());
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
            startForegroundSafe();
        } catch (Throwable ignored) {}
    }

    private void startForegroundSafe() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                NotificationManager nm = getSystemService(NotificationManager.class);
                if (nm != null && nm.getNotificationChannel(CH_ID) == null) {
                    NotificationChannel ch = new NotificationChannel(
                        CH_ID, "音乐播放", NotificationManager.IMPORTANCE_LOW);
                    ch.setShowBadge(false);
                    nm.createNotificationChannel(ch);
                }
            }
            startForeground(1, buildNotif());
        } catch (Throwable ignored) {}
    }

    private void updateNotif() {
        try {
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.notify(1, buildNotif());
        } catch (Throwable ignored) {}
    }

    private Notification buildNotif() {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pi = PendingIntent.getActivity(this, 0,
            new Intent(this, PlayerActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP),
            flags);

        String title = "MUSIC·Pro", text = "已暂停";
        try {
            MediaItem it = sharedPlayer != null ? sharedPlayer.getCurrentMediaItem() : null;
            if (it != null && it.mediaMetadata != null) {
                MediaMetadata md = it.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) text = md.artist.toString();
            }
            if (sharedPlayer != null && sharedPlayer.isPlaying()) text += " · 播放中";
        } catch (Throwable ignored) {}

        // 用全限定名，避免和 androidx.media.app.NotificationCompat 冲突
        androidx.core.app.NotificationCompat.Builder b =
            new androidx.core.app.NotificationCompat.Builder(this, CH_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pi)
            .setOngoing(true)
            .setVisibility(androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_TRANSPORT);

        try {
            androidx.media.app.NotificationCompat.MediaStyle style =
                new androidx.media.app.NotificationCompat.MediaStyle();
            style.setShowActionsInCompactView(0, 1, 2);
            b.setStyle(style);
        } catch (Throwable ignored) {}

        try {
            PendingIntent prevPi = PendingIntent.getService(this, 11,
                new Intent(this, MusicService.class).setAction("PREV"), flags);
            PendingIntent togglePi = PendingIntent.getService(this, 10,
                new Intent(this, MusicService.class).setAction("TOGGLE"), flags);
            PendingIntent nextPi = PendingIntent.getService(this, 12,
                new Intent(this, MusicService.class).setAction("NEXT"), flags);
            b.addAction(android.R.drawable.ic_media_previous, "上一首", prevPi);
            b.addAction(sharedPlayer != null && sharedPlayer.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play, "播放/暂停", togglePi);
            b.addAction(android.R.drawable.ic_media_next, "下一首", nextPi);
        } catch (Throwable ignored) {}
        return b.build();
    }

    @Nullable @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo ci) { return session; }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String a = intent.getAction();
            if ("TOGGLE".equals(a)) toggle(getApplicationContext());
            else if ("NEXT".equals(a)) next(getApplicationContext());
            else if ("PREV".equals(a)) prev(getApplicationContext());
        }
        return super.onStartCommand(intent, flags, startId);
    }

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
                        .setTitle(sg.title).setArtist(sg.artist).build())
                    .build());
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
                u = "https://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
            return u;
        }
        return "file://" + sg.path;
    }

    public static void play(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(ctx, sharedIndex, true); return;
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
            rebuild(ctx, sharedIndex, true); return;
        }
        if (sharedPlayer.isPlaying()) sharedPlayer.pause();
        else sharedPlayer.play();
    }
    public static void next(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        int total = sharedQueue.size();
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        int ni = idx < total - 1 ? idx + 1 : 0;
        gotoIdx(ctx, ni);
    }
    public static void prev(Context ctx) {
        ensure(ctx);
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        int idx = sharedPlayer.getCurrentMediaItemIndex();
        int pi = idx > 0 ? idx - 1 : 0;
        gotoIdx(ctx, pi);
    }
    private static void gotoIdx(Context ctx, final int ni) {
        if (sharedPlayer == null || sharedQueue.isEmpty()) return;
        sharedIndex = ni;
        final Song sg = sharedQueue.get(ni);
        if (!sg.isOnline || (sg.onlineUrl != null && !sg.onlineUrl.isEmpty())) {
            try {
                if (sharedPlayer.getMediaItemCount() == 0) rebuild(ctx, ni, true);
                else { sharedPlayer.seekTo(ni, 0); sharedPlayer.play(); }
            } catch (Throwable ignored) {}
            return;
        }
        com.music.app.util.NeteaseApi.getPlayUrl(sg.id, new com.music.app.util.NeteaseApi.OnUrl() {
            @Override public void onResult(String url) {
                if (url != null && !url.isEmpty()) sg.onlineUrl = url;
                rebuild(null, ni, true);
            }
        });
    }
    public static void seekToPos(long ms) { if (sharedPlayer != null) sharedPlayer.seekTo(ms); }
    public static void seekToIdx(int idx, boolean play) {
        if (sharedPlayer == null) return;
        if (sharedPlayer.getMediaItemCount() == 0 && !sharedQueue.isEmpty()) {
            rebuild(null, idx, play); return;
        }
        sharedPlayer.seekTo(idx, 0);
        if (play) sharedPlayer.play();
    }
    private static void ensure(Context ctx) {
        if (!started && ctx != null) {
            try { ctx.startService(new Intent(ctx, MusicService.class)); started = true; }
            catch (Throwable ignored) {}
        }
    }
    @Override public void onDestroy() {
        if (session != null) { session.release(); session = null; }
        super.onDestroy();
    }
}
