package com.music.app.service;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.R;
import com.music.app.util.Prefs;

public class IslandService extends Service {

    private WindowManager wm;
    private View island;
    private WindowManager.LayoutParams lp;
    private ExoPlayer player;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose;
    private ProgressBar progress;
    private View collapsedRoot, expandedRoot;

    private boolean expanded = false;
    private int screenW, screenH, statusBarH;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());
    private ObjectAnimator breathe;

    public static IslandService instance;

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        instance = this;
        measureScreen();
        player = MusicService.sharedPlayer;
        initView();
        startTick();
    }

    private void measureScreen() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        if (Build.VERSION.SDK_INT >= 30) {
            WindowManager w = (WindowManager) getSystemService(WINDOW_SERVICE);
            Rect b = w.getCurrentWindowMetrics().getBounds();
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

        lp = new WindowManager.LayoutParams(
            getIslandWidthPx(), getIslandHeightPx(),
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);

        wm.addView(island, lp);

        tvTitle = island.findViewById(R.id.islandTitle);
        tvArtist = island.findViewById(R.id.islandArtist);
        tvCurrent = island.findViewById(R.id.islandCurrent);
        tvTotal = island.findViewById(R.id.islandTotal);
        btnPlay = island.findViewById(R.id.islandPlay);
        btnPrev = island.findViewById(R.id.islandPrev);
        btnNext = island.findViewById(R.id.islandNext);
        btnClose = island.findViewById(R.id.islandClose);
        progress = island.findViewById(R.id.islandProgress);
        collapsedRoot = island.findViewById(R.id.islandCollapsed);
        expandedRoot = island.findViewById(R.id.islandExpanded);

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                if (player != null) {
                    if (player.isPlaying()) player.pause(); else player.play();
                }
            }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                if (player != null && player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                    player.play();
                }
            }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                if (player != null && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    player.play();
                }
            }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });
        if (collapsedRoot != null) collapsedRoot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleExpand(); }
        });

        island.setAlpha(0f);
        island.setScaleX(0.6f);
        island.setScaleY(0.6f);
        island.setTranslationY(-40f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setDuration(520).setInterpolator(new OvershootInterpolator(1.3f)).start();
    }

    public int getIslandWidthPx() {
        float w = Prefs.islandWidth(this);
        if (w < 0.2f) w = 0.2f;
        if (w > 0.9f) w = 0.9f;
        return (int)(screenW * w);
    }

    public int getIslandHeightPx() {
        float dp = Prefs.islandHeight(this);
        if (dp < 40f) dp = 40f;
        if (dp > 120f) dp = 120f;
        return (int)(dp * density);
    }

    public void applySize() {
        if (island == null || wm == null || lp == null) return;
        int newW = expanded ? (int)(screenW * 0.86f) : getIslandWidthPx();
        int newH = expanded ? (int)(120 * density) : getIslandHeightPx();
        ValueAnimator wa = ValueAnimator.ofInt(lp.width, newW);
        ValueAnimator ha = ValueAnimator.ofInt(lp.height, newH);
        wa.setDuration(250); ha.setDuration(250);
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
        wa.start(); ha.start();
    }

    private void toggleExpand() {
        expanded = !expanded;
        float toScale = expanded ? 1.05f : 1f;
        island.animate().scaleX(toScale).scaleY(toScale)
            .setDuration(320).setInterpolator(new OvershootInterpolator(1.4f)).start();
        if (expandedRoot != null) {
            if (expanded) {
                expandedRoot.setVisibility(View.VISIBLE);
                expandedRoot.setAlpha(0f);
                expandedRoot.setTranslationY(-10f);
                expandedRoot.animate().alpha(1f).translationY(0f)
                    .setDuration(280).setInterpolator(new DecelerateInterpolator()).start();
            } else {
                expandedRoot.animate().alpha(0f).setDuration(180)
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            expandedRoot.setVisibility(View.GONE);
                        }
                    }).start();
            }
        }
        int targetW = expanded ? (int)(screenW * 0.86f) : getIslandWidthPx();
        int targetH = expanded ? (int)(120 * density) : getIslandHeightPx();
        ValueAnimator va = ValueAnimator.ofInt(lp.width, targetW);
        va.setDuration(280);
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.width = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        va.start();
        ValueAnimator vb = ValueAnimator.ofInt(lp.height, targetH);
        vb.setDuration(280);
        vb.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.height = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
            }
        });
        vb.start();
    }

    private void bounce(View v) {
        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70).start();
        v.postDelayed(new Runnable() {
            @Override public void run() {
                v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(new OvershootInterpolator(2.5f)).start();
            }
        }, 70);
    }

    private void startTick() {
        h.post(new Runnable() {
            @Override public void run() {
                updateUi();
                h.postDelayed(this, 400);
            }
        });
    }

    private void updateUi() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player == null) return;
        try {
            MediaItem item = player.getCurrentMediaItem();
            if (item != null && tvTitle != null) tvTitle.setText("♪ 正在播放");
            if (btnPlay != null) {
                btnPlay.setImageResource(player.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
            long pos = player.getCurrentPosition();
            long dur = player.getDuration();
            if (dur > 0 && progress != null) {
                progress.setMax(1000);
                progress.setProgress((int)(pos * 1000 / dur));
            }
            if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
            if (tvTotal != null) tvTotal.setText(fmt((int) dur));
            if (breathe == null && island != null && player.isPlaying()) {
                breathe = ObjectAnimator.ofFloat(island, "alpha", 1f, 0.88f, 1f);
                breathe.setDuration(1800);
                breathe.setRepeatCount(ObjectAnimator.INFINITE);
                breathe.setInterpolator(new AccelerateDecelerateInterpolator());
                breathe.start();
            }
            if (breathe != null && !player.isPlaying()) {
                breathe.cancel(); breathe = null; island.setAlpha(1f);
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
        if (breathe != null) breathe.cancel();
        if (island != null && wm != null) {
            try { wm.removeView(island); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
