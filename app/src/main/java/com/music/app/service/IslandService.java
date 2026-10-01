package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.LyricsParser;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class IslandService extends Service {

    private WindowManager wm;
    private View island;
    private WindowManager.LayoutParams lp;
    private ExoPlayer player;
    private TextView tvTitle, tvExpTitle, tvExpArtist, tvCurrent, tvTotal;
    private TextView tvLyric1, tvLyric2, tvLyric3;
    private ImageView ivCover, ivExpCover;
    private ImageButton btnPlay, btnPlayExp, btnPrev, btnNext, btnClose;
    private SeekBar progress;
    private View collapsedRoot, expandedRoot;

    private boolean expanded = false;
    private boolean dragging = false;
    private boolean animating = false;
    private int screenW, screenH, statusBarH;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Vibrator vib;

    // 老设备优化
    private boolean lowEnd = false;

    private long lastSongId = -1;
    private String lastTitle = "";
    private String lastArtist = "";
    private boolean lastPlaying = false;
    private String lastLyric = "";
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();

    public static IslandService instance;
    public static List<Song> queue;

    private final PathInterpolator iosSpring =
        new PathInterpolator(0.25f, 0.1f, 0.25f, 1.0f);
    private final PathInterpolator iosEaseOut =
        new PathInterpolator(0.0f, 0.0f, 0.2f, 1.0f);

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        lowEnd = Build.VERSION.SDK_INT < 26; // Android 8.1 及以下视为低端
        measure();
        player = MusicService.sharedPlayer;
        if (queue == null) queue = MusicService.sharedQueue;
        initView();
        h.post(tick);
    }

    private void measure() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = ((WindowManager) getSystemService(WINDOW_SERVICE)).getCurrentWindowMetrics().getBounds();
            screenW = b.width(); screenH = b.height();
        } else {
            screenW = dm.widthPixels; screenH = dm.heightPixels;
        }
        int rid = getResources().getIdentifier("status_bar_height", "dimen", "android");
        statusBarH = rid > 0 ? getResources().getDimensionPixelSize(rid) : (int)(24 * density);
    }

    private void initView() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        island = LayoutInflater.from(this).inflate(R.layout.view_island, null);

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
            getW(), getH(), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(8 * density);
        wm.addView(island, lp);

        // ★ 老设备不开硬件加速层，避免闪烁
        if (!lowEnd) island.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        tvTitle = island.findViewById(R.id.islandTitle);
        tvExpTitle = island.findViewById(R.id.islandExpTitle);
        tvExpArtist = island.findViewById(R.id.islandExpArtist);
        tvLyric1 = island.findViewById(R.id.islandLyric1);
        tvLyric2 = island.findViewById(R.id.islandLyric2);
        tvLyric3 = island.findViewById(R.id.islandLyric3);
        tvCurrent = island.findViewById(R.id.islandCurrent);
        tvTotal = island.findViewById(R.id.islandTotal);
        ivCover = island.findViewById(R.id.islandCover);
        ivExpCover = island.findViewById(R.id.islandExpCover);
        btnPlay = island.findViewById(R.id.islandPlay);
        btnPlayExp = island.findViewById(R.id.islandPlayExpand);
        btnPrev = island.findViewById(R.id.islandPrev);
        btnNext = island.findViewById(R.id.islandNext);
        btnClose = island.findViewById(R.id.islandClose);
        progress = island.findViewById(R.id.islandProgress);
        collapsedRoot = island.findViewById(R.id.islandCollapsed);
        expandedRoot = island.findViewById(R.id.islandExpanded);

        bindClick(btnPlay, new Runnable() { @Override public void run() { togglePlay(); } });
        bindClick(btnPlayExp, new Runnable() { @Override public void run() { togglePlay(); } });
        bindClick(btnPrev, new Runnable() { @Override public void run() { prevSong(); } });
        bindClick(btnNext, new Runnable() { @Override public void run() { nextSong(); } });
        bindClick(btnClose, new Runnable() { @Override public void run() { stopSelf(); } });

        if (collapsedRoot != null) {
            collapsedRoot.setClickable(true);
            collapsedRoot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (!expanded) expand(); }
            });
        }
        if (expandedRoot != null) {
            expandedRoot.setClickable(true);
            expandedRoot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (expanded) collapse(); }
            });
        }

        if (progress != null) {
            progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                    if (fu && player != null) {
                        long dur = player.getDuration();
                        if (dur > 0) player.seekTo(dur * p / 1000);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) { dragging = false; }
            });
        }

        island.setAlpha(0f);
        island.setScaleX(0.7f);
        island.setScaleY(0.7f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(lowEnd ? 250 : 450).setInterpolator(iosSpring).start();
    }

    private void bindClick(View v, final Runnable action) {
        if (v == null) return;
        v.setClickable(true);
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View view) {
                if (vib != null) { try { vib.vibrate(12); } catch (Throwable ignored) {} }
                view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(60)
                    .setInterpolator(iosEaseOut).start();
                view.postDelayed(new Runnable() {
                    @Override public void run() {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(150)
                            .setInterpolator(iosSpring).start();
                    }
                }, 60);
                action.run();
            }
        });
    }

    private void ensurePlayer() {
        if (player == null || player != MusicService.sharedPlayer) {
            player = MusicService.sharedPlayer;
        }
        if (player == null) {
            try {
                Intent svc = new Intent(this, MusicService.class);
                startService(svc);
            } catch (Throwable ignored) {}
            player = MusicService.sharedPlayer;
        }
        if (queue == null) queue = MusicService.sharedQueue;
    }

    private void togglePlay() {
        ensurePlayer();
        if (player == null) return;
        // 如果队列为空，尝试从 queue 恢复
        if (player.getMediaItemCount() == 0 && queue != null && !queue.isEmpty()) {
            rebuildQueue();
        }
        if (player.isPlaying()) player.pause(); else player.play();
    }

    private void prevSong() {
        ensurePlayer();
        if (player == null) return;
        if (player.getMediaItemCount() == 0 && queue != null && !queue.isEmpty()) {
            rebuildQueue();
            return;
        }
        int idx = player.getCurrentMediaItemIndex();
        if (idx > 0) player.seekTo(idx - 1, 0);
        else player.seekTo(0, 0);
        player.play();
    }

    private void nextSong() {
        ensurePlayer();
        if (player == null) return;
        if (player.getMediaItemCount() == 0 && queue != null && !queue.isEmpty()) {
            rebuildQueue();
            return;
        }
        int total = player.getMediaItemCount();
        int idx = player.getCurrentMediaItemIndex();
        if (idx < total - 1) player.seekTo(idx + 1, 0);
        else player.seekTo(0, 0);
        player.play();
    }

    private void rebuildQueue() {
        if (player == null || queue == null || queue.isEmpty()) return;
        try {
            List<MediaItem> items = new ArrayList<MediaItem>();
            for (int i = 0; i < queue.size(); i++) {
                Song sg = queue.get(i);
                String uri;
                if (sg.isOnline) {
                    uri = sg.onlineUrl;
                    if (uri == null || uri.isEmpty())
                        uri = "https://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
                } else {
                    uri = "file://" + sg.path;
                }
                items.add(new MediaItem.Builder()
                    .setUri(uri)
                    .setMediaMetadata(new MediaMetadata.Builder()
                        .setTitle(sg.title).setArtist(sg.artist).build())
                    .build());
            }
            int idx = MusicService.sharedIndex;
            player.setMediaItems(items, idx, 0);
            player.prepare();
            player.play();
        } catch (Throwable ignored) {}
    }

    private int getW() {
        float w = Prefs.islandWidth(this);
        if (w < 0.2f) w = 0.2f;
        if (w > 0.9f) w = 0.9f;
        return (int)(screenW * w);
    }
    private int getH() {
        float dp = Prefs.islandHeight(this);
        if (dp < 40f) dp = 40f;
        if (dp > 120f) dp = 120f;
        return (int)(dp * density);
    }

    public void applySize() {
        if (island == null || wm == null || expanded) return;
        lp.width = getW();
        lp.height = getH();
        try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
    }

    /** 老设备用普通 DecelerateInterpolator，新设备用 iOS 曲线 */
    private android.view.animation.Interpolator pickIn() {
        return lowEnd ? new DecelerateInterpolator(1.2f) : iosSpring;
    }
    private android.view.animation.Interpolator pickOut() {
        return lowEnd ? new DecelerateInterpolator(1.2f) : iosEaseOut;
    }

    private void expand() {
        if (expanded || animating) return;
        expanded = true;
        animating = true;

        final int startW = lp.width, startH = lp.height;
        final int targetW = (int)(screenW * 0.88f);
        final int targetH = (int)(210 * density);

        expandedRoot.setVisibility(View.VISIBLE);
        expandedRoot.setAlpha(0f);

        // 老设备动画时长减半
        int dur = lowEnd ? 260 : 500;

        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
        ValueAnimator ha = ValueAnimator.ofInt(startH, targetH);
        wa.setDuration(dur); ha.setDuration(dur);
        wa.setInterpolator(pickIn()); ha.setInterpolator(pickIn());

        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.width = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.height = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator a) {
                collapsedRoot.setVisibility(View.GONE);
            }
            @Override public void onAnimationEnd(Animator a) {
                animating = false;
                expandedRoot.setAlpha(0f);
                expandedRoot.animate().alpha(1f).setDuration(180)
                    .setInterpolator(pickOut()).start();
            }
        });
        wa.start(); ha.start();
    }

    private void collapse() {
        if (!expanded || animating) return;
        expanded = false;
        animating = true;

        expandedRoot.animate().alpha(0f).setDuration(140).start();

        final int startW = lp.width, startH = lp.height;
        final int targetW = getW(), targetH = getH();
        int dur = lowEnd ? 220 : 400;

        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
        ValueAnimator ha = ValueAnimator.ofInt(startH, targetH);
        wa.setDuration(dur); ha.setDuration(dur);
        wa.setInterpolator(pickOut()); ha.setInterpolator(pickOut());
        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.width = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.height = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                expandedRoot.setVisibility(View.GONE);
                collapsedRoot.setVisibility(View.VISIBLE);
                collapsedRoot.setAlpha(0f);
                collapsedRoot.animate().alpha(1f).setDuration(180)
                    .setInterpolator(pickOut()).start();
                animating = false;
            }
        });
        wa.start(); ha.start();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            h.postDelayed(this, lowEnd ? 900 : 500);
        }
    };

    private void update() {
        ensurePlayer();
        if (player == null) return;
        try {
            MediaItem item = player.getCurrentMediaItem();
            long songId = 0;
            String title = "正在播放";
            String artist = "MUSIC·Pro";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }
            int idx = player.getCurrentMediaItemIndex();
            MusicService.sharedIndex = idx;
            if (queue != null && idx >= 0 && idx < queue.size()) {
                songId = queue.get(idx).id;
            }
            if (!title.equals(lastTitle)) {
                if (tvTitle != null) tvTitle.setText(title);
                if (tvExpTitle != null) tvExpTitle.setText(title);
                lastTitle = title;
            }
            if (!artist.equals(lastArtist)) {
                if (tvExpArtist != null) tvExpArtist.setText(artist);
                lastArtist = artist;
            }
            if (songId != lastSongId) {
                lastSongId = songId;
                String name = WallpaperHelper.forSong(this, songId);
                Bitmap bm = WallpaperHelper.loadSmall(this, name);
                if (bm != null) {
                    if (ivCover != null) ivCover.setImageBitmap(bm);
                    if (ivExpCover != null) ivExpCover.setImageBitmap(bm);
                }
                lyricLines = new ArrayList<LyricsParser.Line>();
                lastLyric = "";
            }

            // 三行歌词
            if (queue != null && idx >= 0 && idx < queue.size()) {
                String lrc = queue.get(idx).lyric;
                if (lrc != null && !lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                if (!lyricLines.isEmpty()) {
                    long pos = player.getCurrentPosition();
                    int li = LyricsParser.findIndex(lyricLines, pos);
                    String prev = li > 0 ? lyricLines.get(li-1).text : "";
                    String cur = (li >= 0 && li < lyricLines.size()) ? lyricLines.get(li).text : "";
                    String next = (li+1 < lyricLines.size()) ? lyricLines.get(li+1).text : "";
                    if (tvLyric1 != null && !prev.equals(tvLyric1.getText().toString())) tvLyric1.setText(prev);
                    if (tvLyric2 != null && !cur.equals(tvLyric2.getText().toString())) tvLyric2.setText(cur);
                    if (tvLyric3 != null && !next.equals(tvLyric3.getText().toString())) tvLyric3.setText(next);
                }
            }

            boolean playing = player.isPlaying();
            if (playing != lastPlaying) {
                lastPlaying = playing;
                int icon = playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play;
                if (btnPlay != null) btnPlay.setImageResource(icon);
                if (btnPlayExp != null) btnPlayExp.setImageResource(icon);
            }
            if (expanded && !dragging && progress != null) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0) {
                    progress.setMax(1000);
                    progress.setProgress((int)(pos * 1000 / dur));
                    if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
                    if (tvTotal != null) tvTotal.setText(fmt((int) dur));
                }
            }
        } catch (Throwable ignored) {}
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override public void onDestroy() {
        instance = null;
        h.removeCallbacksAndMessages(null);
        if (island != null && wm != null) {
            try { wm.removeView(island); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
