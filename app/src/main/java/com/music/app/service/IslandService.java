package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
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
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private TextView tvExpTitle, tvExpArtist;
    private ImageView ivCover, ivExpCover;
    private ImageButton btnPlay, btnPlayExp, btnPrev, btnNext, btnPrevMini, btnNextMini, btnClose;
    private SeekBar progress;
    private View collapsedRoot, expandedRoot;

    private boolean expanded = false;
    private boolean dragging = false;
    private int screenW, screenH, statusBarH;
    private float density;
    private final Handler h = new Handler(Looper.getMainLooper());
    private ObjectAnimator breathe;

    public static IslandService instance;
    public static List<Song> queue;
    private int primaryColor = 0xFFFF6B9D;

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
            getIslandWidthPx(), getIslandHeightPx(), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);
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
        btnPrevMini = island.findViewById(R.id.islandPrevMini);
        btnNextMini = island.findViewById(R.id.islandNextMini);
        btnClose = island.findViewById(R.id.islandClose);
        progress = island.findViewById(R.id.islandProgress);
        collapsedRoot = island.findViewById(R.id.islandCollapsed);
        expandedRoot = island.findViewById(R.id.islandExpanded);

        View.OnClickListener playClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                android.util.Log.d("Island", "click PLAY");
                bounce(v);
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null) {
                    if (player.isPlaying()) player.pause(); else player.play();
                }
            }
        };
        View.OnClickListener prevClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                android.util.Log.d("Island", "click PREV");
                bounce(v);
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null && player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem();
                    player.play();
                }
            }
        };
        View.OnClickListener nextClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
                android.util.Log.d("Island", "click NEXT");
                bounce(v);
                if (player == null) player = MusicService.sharedPlayer;
                if (player != null && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem();
                    player.play();
                }
            }
        };
        if (btnPlay != null) btnPlay.setOnClickListener(playClick);
        if (btnPlayExp != null) btnPlayExp.setOnClickListener(playClick);
        if (btnPrev != null) btnPrev.setOnClickListener(prevClick);
        if (btnNext != null) btnNext.setOnClickListener(nextClick);
        if (btnPrevMini != null) btnPrevMini.setOnClickListener(prevClick);
        if (btnNextMini != null) btnNextMini.setOnClickListener(nextClick);
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });
        if (collapsedRoot != null) collapsedRoot.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleExpand(); }
        });
        // ★ 展开态点击空白区域 → 收起
        if (expandedRoot != null) {
            expandedRoot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { toggleExpand(); }
            });
        }

        if (progress != null) {
            progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    if (fromUser && player != null) {
                        long dur = player.getDuration();
                        long newPos = dur * p / 1000;
                        player.seekTo(newPos);
                        if (tvCurrent != null) tvCurrent.setText(fmt((int) newPos));
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) { dragging = false; }
            });
        }

        // 高光扫过动画
        final View shine = island.findViewById(R.id.islandShine);
        if (shine != null) {
            shine.setAlpha(0f);
            ObjectAnimator shineAnim = ObjectAnimator.ofFloat(shine, "alpha", 0f, 0.6f, 0f);
            shineAnim.setDuration(2500);
            shineAnim.setRepeatCount(ObjectAnimator.INFINITE);
            
            shineAnim.setInterpolator(new AccelerateDecelerateInterpolator());
            shineAnim.start();
        }

        // 手势：向上滑收起
        if (island != null) {
            island.setOnTouchListener(new View.OnTouchListener() {
                private float startY = 0;
                @Override public boolean onTouch(View v, android.view.MotionEvent e) {
                    switch (e.getAction()) {
                        case android.view.MotionEvent.ACTION_DOWN:
                            startY = e.getRawY();
                            return false; // 不拦截，让子 view 处理
                        case android.view.MotionEvent.ACTION_UP:
                            float dy = e.getRawY() - startY;
                            if (expanded && dy < -50 && Math.abs(dy) > 60) {
                                toggleExpand();
                                return true;
                            }
                            return false;
                    }
                    return false;
                }
            });
        }

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
        int newW = expanded ? (int)(screenW * 0.88f) : getIslandWidthPx();
        int newH = expanded ? (int)(186 * density) : getIslandHeightPx();
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
        if (expanded) {
            // 展开：先左右 → 再上下 → 微调宽
            final int midW = (int)(screenW * 0.65f);
            final int targetW = (int)(screenW * 0.90f);
            final int targetH = (int)(186 * density);

            ValueAnimator wa = ValueAnimator.ofInt(lp.width, midW);
            wa.setDuration(280);
            wa.setInterpolator(new OvershootInterpolator(1.2f));
            wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    lp.width = (int) a.getAnimatedValue();
                    try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                }
            });
            wa.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    ValueAnimator ha = ValueAnimator.ofInt(lp.height, targetH);
                    ha.setDuration(280);
                    ha.setInterpolator(new OvershootInterpolator(1.2f));
                    ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override public void onAnimationUpdate(ValueAnimator a) {
                            lp.height = (int) a.getAnimatedValue();
                            try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                        }
                    });
                    ha.addListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) {
                            ValueAnimator wa2 = ValueAnimator.ofInt(lp.width, targetW);
                            wa2.setDuration(180);
                            wa2.setInterpolator(new DecelerateInterpolator());
                            wa2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override public void onAnimationUpdate(ValueAnimator a) {
                                    lp.width = (int) a.getAnimatedValue();
                                    try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                                }
                            });
                            wa2.start();
                        }
                    });
                    ha.start();
                }
            });
            wa.start();

            if (collapsedRoot != null) collapsedRoot.setVisibility(View.GONE);
            if (expandedRoot != null) {
                expandedRoot.setVisibility(View.VISIBLE);
                expandedRoot.setAlpha(0f);
                expandedRoot.setScaleX(0.85f);
                expandedRoot.setScaleY(0.85f);
                expandedRoot.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(180).setDuration(420)
                    .setInterpolator(new OvershootInterpolator(1.2f)).start();
            }
        } else {
            // 收起：反向
            final int collapsedW = getIslandWidthPx();
            final int collapsedH = getIslandHeightPx();
            final int midH = (int)(60 * density);

            if (expandedRoot != null) {
                expandedRoot.animate().alpha(0f).scaleX(0.85f).scaleY(0.85f)
                    .setDuration(200).setInterpolator(new AccelerateInterpolator())
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            expandedRoot.setVisibility(View.GONE);
                            if (collapsedRoot != null) collapsedRoot.setVisibility(View.VISIBLE);
                        }
                    }).start();
            }

            ValueAnimator ha = ValueAnimator.ofInt(lp.height, midH);
            ha.setDuration(200);
            ha.setInterpolator(new AccelerateInterpolator());
            ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    lp.height = (int) a.getAnimatedValue();
                    try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                }
            });
            ha.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    ValueAnimator wa = ValueAnimator.ofInt(lp.width, collapsedW);
                    wa.setDuration(200);
                    wa.setInterpolator(new DecelerateInterpolator());
                    wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                        @Override public void onAnimationUpdate(ValueAnimator a) {
                            lp.width = (int) a.getAnimatedValue();
                            try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                        }
                    });
                    wa.addListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) {
                            ValueAnimator ha2 = ValueAnimator.ofInt(lp.height, collapsedH);
                            ha2.setDuration(200);
                            ha2.setInterpolator(new DecelerateInterpolator());
                            ha2.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                @Override public void onAnimationUpdate(ValueAnimator a) {
                                    lp.height = (int) a.getAnimatedValue();
                                    try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
                                }
                            });
                            ha2.start();
                        }
                    });
                    wa.start();
                }
            });
            ha.start();
        }
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
            int idx = player.getCurrentMediaItemIndex();
            String title = "♪ 正在播放";
            String artist = "MUSIC·Pro";
            long songId = 0;
            if (queue != null && idx >= 0 && idx < queue.size()) {
                Song song = queue.get(idx);
                title = song.title;
                artist = song.artist;
                songId = song.id;
            }
            if (tvTitle != null) tvTitle.setText(title);
            if (tvArtist != null) tvArtist.setText(artist);
            if (tvExpTitle != null) tvExpTitle.setText(title);
            if (tvExpArtist != null) tvExpArtist.setText(artist);

            String name = WallpaperHelper.forSong(this, songId);
            Bitmap bm = WallpaperHelper.loadSmall(this, name);
            if (bm != null) {
                if (ivCover != null) ivCover.setImageBitmap(bm);
                if (ivExpCover != null) ivExpCover.setImageBitmap(bm);
            }

            int icon = player.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play;
            if (btnPlay != null) btnPlay.setImageResource(icon);
            if (btnPlayExp != null) btnPlayExp.setImageResource(icon);

            long pos = player.getCurrentPosition();
            long dur = player.getDuration();
            if (!dragging && dur > 0 && progress != null) {
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

    private int extractColor(Bitmap bm) {
        if (bm == null) return 0xFFFF6B9D;
        try {
            int w = bm.getWidth(), h = bm.getHeight();
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int y = 0; y < h; y += 4) {
                for (int x = 0; x < w; x += 4) {
                    int px = bm.getPixel(x, y);
                    r += (px >> 16) & 0xFF;
                    g += (px >> 8) & 0xFF;
                    b += px & 0xFF;
                    n++;
                }
            }
            if (n == 0) return 0xFFFF6B9D;
            int rr = (int)(r / n), gg = (int)(g / n), bb = (int)(b / n);
            return 0xFF000000 | (rr << 16) | (gg << 8) | bb;
        } catch (Throwable t) { return 0xFFFF6B9D; }
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
