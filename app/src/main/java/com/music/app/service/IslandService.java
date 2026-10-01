package com.music.app.service;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.List;

public class IslandService extends Service {

    private WindowManager wm;
    private View island;
    private WindowManager.LayoutParams lp;
    private ExoPlayer player;
    private TextView tvTitle, tvArtist, tvExpTitle, tvExpArtist, tvCurrent, tvTotal;
    private ImageView ivCover, ivExpCover;
    private ImageButton btnPlay, btnPlayExp, btnPrev, btnNext, btnClose;
    private SeekBar progress;
    private View collapsedRoot, expandedRoot;

    private boolean expanded = false;
    private boolean dragging = false;
    private int screenW, statusBarH;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());

    // 缓存
    private long lastSongId = -1;
    private String lastTitle = "";
    private String lastArtist = "";
    private int lastProgress = -1;
    private boolean lastPlaying = false;

    public static IslandService instance;
    public static List<Song> queue;

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        measure();
        player = MusicService.sharedPlayer;
        initView();
        h.post(tick);
    }

    private void measure() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = ((WindowManager) getSystemService(WINDOW_SERVICE)).getCurrentWindowMetrics().getBounds();
            screenW = b.width();
        } else {
            screenW = dm.widthPixels;
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
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);

        wm.addView(island, lp);

        // 强制硬件层，动画更顺
        island.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        tvTitle = island.findViewById(R.id.islandTitle);
        tvArtist = island.findViewById(R.id.islandArtist);
        tvExpTitle = island.findViewById(R.id.islandExpTitle);
        tvExpArtist = island.findViewById(R.id.islandExpArtist);
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

        View.OnClickListener playClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null) {
                    if (player.isPlaying()) player.pause(); else player.play();
                }
            }
        };
        View.OnClickListener prevClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null && player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                    player.play();
                }
            }
        };
        View.OnClickListener nextClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    player.play();
                }
            }
        };
        btnPlay.setOnClickListener(playClick);
        btnPlayExp.setOnClickListener(playClick);
        btnPrev.setOnClickListener(prevClick);
        btnNext.setOnClickListener(nextClick);
        btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });
        collapsedRoot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });
        expandedRoot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggle(); }
        });

        progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                if (fu && player != null) {
                    long dur = player.getDuration();
                    player.seekTo(dur * p / 1000);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) { dragging = false; }
        });

        island.setAlpha(0f);
        island.setScaleX(0.7f);
        island.setScaleY(0.7f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400).setInterpolator(new OvershootInterpolator(1.2f)).start();
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
        if (island == null || wm == null) return;
        lp.width = expanded ? (int)(screenW * 0.88f) : getW();
        lp.height = expanded ? (int)(170 * density) : getH();
        try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
    }

    private void toggle() {
        expanded = !expanded;
        if (expanded) {
            collapsedRoot.setVisibility(View.GONE);
            expandedRoot.setVisibility(View.VISIBLE);
            expandedRoot.setAlpha(0f);
            expandedRoot.animate().alpha(1f).setDuration(200).start();
            lp.width = (int)(screenW * 0.88f);
            lp.height = (int)(170 * density);
        } else {
            expandedRoot.setVisibility(View.GONE);
            collapsedRoot.setVisibility(View.VISIBLE);
            lp.width = getW();
            lp.height = getH();
        }
        try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            h.postDelayed(this, 800);
        }
    };

    private void update() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player == null) return;
        try {
            int idx = player.getCurrentMediaItemIndex();
            long songId = 0;
            String title = "♪ 正在播放";
            String artist = "MUSIC·Pro";
            if (queue != null && idx >= 0 && idx < queue.size()) {
                Song s = queue.get(idx);
                songId = s.id;
                title = s.title;
                artist = s.artist;
            }

            // 只在变化时更新文字
            if (!title.equals(lastTitle)) {
                if (tvTitle != null) tvTitle.setText(title);
                if (tvExpTitle != null) tvExpTitle.setText(title);
                lastTitle = title;
            }
            if (!artist.equals(lastArtist)) {
                if (tvArtist != null) tvArtist.setText(artist);
                if (tvExpArtist != null) tvExpArtist.setText(artist);
                lastArtist = artist;
            }

            // 只在换歌时更新封面
            if (songId != lastSongId) {
                lastSongId = songId;
                String name = WallpaperHelper.forSong(this, songId);
                Bitmap bm = WallpaperHelper.loadSmall(this, name);
                if (bm != null) {
                    if (ivCover != null) ivCover.setImageBitmap(bm);
                    if (ivExpCover != null) ivExpCover.setImageBitmap(bm);
                }
            }

            // 只在播放状态变化时更新图标
            boolean playing = player.isPlaying();
            if (playing != lastPlaying) {
                lastPlaying = playing;
                int icon = playing
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play;
                if (btnPlay != null) btnPlay.setImageResource(icon);
                if (btnPlayExp != null) btnPlayExp.setImageResource(icon);
            }

            // 进度只在展开态 + 未拖动时更新
            if (expanded && !dragging) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                if (dur > 0 && progress != null) {
                    int p = (int)(pos * 1000 / dur);
                    if (p != lastProgress) {
                        progress.setMax(1000);
                        progress.setProgress(p);
                        lastProgress = p;
                    }
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
