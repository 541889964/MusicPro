package com.music.app.service;

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
import android.view.MotionEvent;
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
    private boolean animating = false;
    private int screenW, screenH, statusBarH;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());
    private Vibrator vib;

    private long lastSongId = -1;
    private String lastTitle = "";
    private String lastArtist = "";
    private boolean lastPlaying = false;

    public static IslandService instance;
    public static List<Song> queue;

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        vib = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
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
            screenH = b.height();
        } else {
            screenW = dm.widthPixels;
            screenH = dm.heightPixels;
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

        // ★ 关键：去掉 NOT_TOUCH_MODAL，加 WATCH_OUTSIDE_TOUCH
        lp = new WindowManager.LayoutParams(
            getW(), getH(), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(8 * density);

        wm.addView(island, lp);

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

        // 用 OnTouchListener 保证按钮响应
        attachTap(btnPlay, new Runnable() { @Override public void run() { togglePlay(); } });
        attachTap(btnPlayExp, new Runnable() { @Override public void run() { togglePlay(); } });
        attachTap(btnPrev, new Runnable() { @Override public void run() { prevSong(); } });
        attachTap(btnNext, new Runnable() { @Override public void run() { nextSong(); } });
        attachTap(btnClose, new Runnable() { @Override public void run() { stopSelf(); } });

        // 点击空白 → 展开/收起
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
                    if (dur > 0) player.seekTo(dur * p / 1000);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) { dragging = false; }
        });

        island.setAlpha(0f);
        island.setScaleX(0.85f);
        island.setScaleY(0.85f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(450).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    /** 通用点击绑定：按下缩放，抬起触发 */
    private void attachTap(View v, final Runnable action) {
        if (v == null) return;
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View view, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        view.animate().scaleX(0.9f).scaleY(0.9f).setDuration(70).start();
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140)
                            .setInterpolator(new OvershootInterpolator(2f)).start();
                        if (vib != null) { try { vib.vibrate(15); } catch (Throwable ignored) {} }
                        action.run();
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        view.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                        return true;
                }
                return false;
            }
        });
    }

    private void togglePlay() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
    }

    private void prevSong() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player != null && player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem();
            player.play();
        }
    }

    private void nextSong() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player != null && player.hasNextMediaItem()) {
            player.seekToNextMediaItem();
            player.play();
        }
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
        if (island == null || wm == null || animating) return;
        lp.width = expanded ? (int)(screenW * 0.88f) : getW();
        lp.height = expanded ? (int)(200 * density) : getH();
        try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
    }

    /** ★ 平滑展开/收起动画（宽高同时过渡） */
    private void toggle() {
        if (animating) return;
        animating = true;
        expanded = !expanded;

        final int startW = lp.width;
        final int startH = lp.height;
        final int endW = expanded ? (int)(screenW * 0.88f) : getW();
        final int endH = expanded ? (int)(200 * density) : getH();

        // 内容切换
        if (expanded) {
            expandedRoot.setVisibility(View.VISIBLE);
            expandedRoot.setAlpha(0f);
            expandedRoot.setScaleX(0.92f);
            expandedRoot.setScaleY(0.92f);
        }

        ValueAnimator wa = ValueAnimator.ofInt(startW, endW);
        ValueAnimator ha = ValueAnimator.ofInt(startH, endH);
        wa.setDuration(280);
        ha.setDuration(280);
        wa.setInterpolator(new DecelerateInterpolator());
        ha.setInterpolator(new DecelerateInterpolator());

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

        ha.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                animating = false;
                if (expanded) {
                    expandedRoot.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(220).setInterpolator(new OvershootInterpolator(1.2f)).start();
                    collapsedRoot.setVisibility(View.GONE);
                } else {
                    collapsedRoot.setVisibility(View.VISIBLE);
                    collapsedRoot.setAlpha(0f);
                    collapsedRoot.animate().alpha(1f).setDuration(180).start();
                }
            }
        });

        wa.start();
        ha.start();

        if (!expanded) {
            expandedRoot.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f)
                .setDuration(180).start();
        }
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            h.postDelayed(this, 700);
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
            if (songId != lastSongId) {
                lastSongId = songId;
                String name = WallpaperHelper.forSong(this, songId);
                Bitmap bm = WallpaperHelper.loadSmall(this, name);
                if (bm != null) {
                    if (ivCover != null) ivCover.setImageBitmap(bm);
                    if (ivExpCover != null) ivExpCover.setImageBitmap(bm);
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
