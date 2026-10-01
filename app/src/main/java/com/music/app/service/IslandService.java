package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.PlayerActivity;
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.LyricsParser;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class IslandService extends Service {
    private static final String TAG = "Island";
    public static IslandService instance;
    public static String pendingNotifTitle = "";
    public static String pendingNotifText = "";
    public static boolean hasPendingNotif = false;

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams lp;
    private View mainIsland, notifBox;
    private View collapsedBox, expandedBox;
    private ImageView imgCover, imgCoverBig, notifIcon;
    private TextView txtTitle, txtTitleBig, txtArtistBig, txtMiniLyric, txtBattery;
    private TextView notifTitle, notifText;
    private TextView[] lyricViews = new TextView[5];
    private TextView txtTimeCur, txtTimeTot;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose;
    private SeekBar seek;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, dragging = false;
    private boolean notifShowing = false;

    private long lastSongId = -1;
    private String lastLyric = "";
    private boolean lastPlaying = false;
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();

    private ObjectAnimator coverRotate, breathe;
    private android.content.BroadcastReceiver chargeReceiver;
    private boolean charging = false;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable ignored) {}
            h.postDelayed(this, 300);
        }
    };

    private final Runnable notifChecker = new Runnable() {
        @Override public void run() {
            try {
                if (hasPendingNotif && !notifShowing) {
                    showNotification(pendingNotifTitle, pendingNotifText);
                    hasPendingNotif = false;
                }
            } catch (Throwable ignored) {}
            h.postDelayed(this, 500);
        }
    };

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf(); return;
        }
        try {
            instance = this;
            measure();
            initView();
            initChargeReceiver();
            h.post(tick);
            h.post(notifChecker);
        } catch (Throwable t) {
            Log.e(TAG, "init fail", t);
            stopSelf();
        }
    }

    private void measure() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        density = dm.density;
        screenW = dm.widthPixels;
        screenH = dm.heightPixels;
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        statusBarH = id > 0 ? getResources().getDimensionPixelSize(id) : (int)(24 * density);
    }

    private int cW() {
        float w = Prefs.islandWidth(this);
        if (w < 0.3f) w = 0.3f; if (w > 0.95f) w = 0.95f;
        return (int)(screenW * w);
    }
    private int cH() {
        float dp = Prefs.islandHeight(this);
        if (dp < 40) dp = 40; if (dp > 90) dp = 90;
        return (int)(dp * density);
    }
    private int eW() {
        float w = Prefs.islandExpW(this);
        if (w < 60) w = 60; if (w > 100) w = 100;
        return (int)(screenW * w / 100f);
    }

    private void initView() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        root = LayoutInflater.from(this).inflate(R.layout.view_island, null);

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(cW(), cH(), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);

        try { wm.addView(root, lp); }
        catch (Throwable t) { Log.e(TAG, "addView", t); stopSelf(); return; }

        bindViews();
        bindClicks();

        root.setAlpha(0f);
        root.setScaleX(0.85f);
        root.setScaleY(0.85f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private void bindViews() {
        mainIsland = root.findViewById(R.id.mainIsland);
        notifBox = root.findViewById(R.id.notifBox);
        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        imgCover = root.findViewById(R.id.imgCover);
        imgCoverBig = root.findViewById(R.id.imgCoverBig);
        notifIcon = root.findViewById(R.id.notifIcon);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtTitleBig = root.findViewById(R.id.txtTitleBig);
        txtArtistBig = root.findViewById(R.id.txtArtistBig);
        txtMiniLyric = root.findViewById(R.id.txtMiniLyric);
        txtBattery = root.findViewById(R.id.txtBattery);
        notifTitle = root.findViewById(R.id.notifTitle);
        notifText = root.findViewById(R.id.notifText);
        lyricViews[0] = root.findViewById(R.id.lyric1);
        lyricViews[1] = root.findViewById(R.id.lyric2);
        lyricViews[2] = root.findViewById(R.id.lyric3);
        lyricViews[3] = root.findViewById(R.id.lyric4);
        lyricViews[4] = root.findViewById(R.id.lyric5);
        txtTimeCur = root.findViewById(R.id.txtTimeCur);
        txtTimeTot = root.findViewById(R.id.txtTimeTot);
        btnPlay = root.findViewById(R.id.btnPlay);
        btnPlayBig = root.findViewById(R.id.btnPlayBig);
        btnPrev = root.findViewById(R.id.btnPrev);
        btnNext = root.findViewById(R.id.btnNext);
        btnClose = root.findViewById(R.id.btnClose);
        seek = root.findViewById(R.id.seek);
    }

    private void bindClicks() {
        if (collapsedBox != null) collapsedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (!expanded) expand(); }
        });
        if (expandedBox != null) expandedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (expanded) collapse(); }
        });

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); MusicService.toggle(getApplicationContext()); }
        });
        if (btnPlayBig != null) btnPlayBig.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); MusicService.toggle(getApplicationContext()); }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); MusicService.prev(getApplicationContext()); }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { press(v); MusicService.next(getApplicationContext()); }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });

        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    if (u) {
                        long dur = MusicService.getDur();
                        if (dur > 0) MusicService.seekToPos(dur * p / 1000);
                    }
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) { dragging = false; }
            });
        }
    }

    private void press(View v) {
        try {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70).start();
            v.postDelayed(new Runnable() {
                @Override public void run() {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(180)
                        .setInterpolator(new OvershootInterpolator(2.5f)).start();
                }
            }, 70);
        } catch (Throwable ignored) {}
    }

    /** 展开 */
    private void expand() {
        if (expanded || animating) return;
        expanded = true; animating = true;
        try {
            final int startW = lp.width;
            final int targetW = eW();
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            expandedBox.setVisibility(View.VISIBLE);
            expandedBox.setAlpha(0f);
            expandedBox.setScaleX(0.92f);
            expandedBox.setScaleY(0.92f);
            collapsedBox.setVisibility(View.GONE);

            ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
            wa.setDuration(320);
            wa.setInterpolator(new OvershootInterpolator(1.15f));
            wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try {
                        lp.width = (int) a.getAnimatedValue();
                        wm.updateViewLayout(root, lp);
                    } catch (Throwable ignored) {}
                }
            });
            wa.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    expandedBox.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(280).setInterpolator(new OvershootInterpolator(1.2f))
                        .withEndAction(new Runnable() {
                            @Override public void run() { animating = false; }
                        }).start();
                }
            });
            wa.start();
            if (btnPlayBig != null) {
                btnPlayBig.setScaleX(0.7f); btnPlayBig.setScaleY(0.7f);
                btnPlayBig.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(200).setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
            }
        } catch (Throwable t) { Log.e(TAG, "expand", t); animating = false; }
    }

    private void collapse() {
        if (!expanded || animating) return;
        expanded = false; animating = true;
        try {
            expandedBox.animate().alpha(0f).scaleX(0.92f).scaleY(0.92f)
                .setDuration(180).setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        final int startW = lp.width;
                        final int targetW = cW();
                        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
                        wa.setDuration(280);
                        wa.setInterpolator(new DecelerateInterpolator(1.5f));
                        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override public void onAnimationUpdate(ValueAnimator a) {
                                try {
                                    lp.width = (int) a.getAnimatedValue();
                                    wm.updateViewLayout(root, lp);
                                } catch (Throwable ignored) {}
                            }
                        });
                        wa.addListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a) {
                                try { lp.height = cH(); wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
                                expandedBox.setVisibility(View.GONE);
                                collapsedBox.setVisibility(View.VISIBLE);
                                collapsedBox.setAlpha(0f);
                                collapsedBox.animate().alpha(1f).setDuration(180)
                                    .withEndAction(new Runnable() {
                                        @Override public void run() { animating = false; }
                                    }).start();
                            }
                        });
                        wa.start();
                    }
                }).start();
        } catch (Throwable t) { Log.e(TAG, "collapse", t); animating = false; }
    }

    /** ★ 通知分裂：主岛缩到左边 2/3，右边展开通知卡 */
    private void showNotification(String title, String text) {
        if (notifShowing || notifBox == null) return;
        notifShowing = true;
        try {
            notifTitle.setText(title);
            notifText.setText(text);
            notifBox.setVisibility(View.VISIBLE);
            notifBox.setAlpha(0f);
            notifBox.setScaleX(0.8f);

            // 主岛宽度从 1.0 → 0.66
            final int startW = lp.width;
            final int targetW = (int)(screenW * 0.85f); // 整个悬浮窗宽 85%
            final int mainW = (int)(targetW * 0.62f);   // 主岛占 62%
            final int notifW = targetW - mainW - (int)(4 * density);

            // 先把窗口整体加宽
            final int oldW = cW();
            final int totalW = cW() + notifW + (int)(4 * density);

            ValueAnimator wa = ValueAnimator.ofInt(oldW, totalW);
            wa.setDuration(320);
            wa.setInterpolator(new OvershootInterpolator(1.15f));
            wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try {
                        lp.width = (int) a.getAnimatedValue();
                        wm.updateViewLayout(root, lp);
                    } catch (Throwable ignored) {}
                }
            });
            wa.start();

            notifBox.animate().alpha(1f).scaleX(1f)
                .setStartDelay(120).setDuration(280)
                .setInterpolator(new OvershootInterpolator(1.3f)).start();

            // 5 秒后收回
            h.postDelayed(new Runnable() {
                @Override public void run() { hideNotification(); }
            }, 5000);
        } catch (Throwable t) { Log.e(TAG, "notif", t); notifShowing = false; }
    }

    private void hideNotification() {
        if (!notifShowing) return;
        try {
            notifBox.animate().alpha(0f).scaleX(0.8f)
                .setDuration(220).setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        notifBox.setVisibility(View.GONE);
                        notifShowing = false;
                        // 窗口缩回
                        final int startW = lp.width;
                        final int targetW = cW();
                        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
                        wa.setDuration(280);
                        wa.setInterpolator(new DecelerateInterpolator(1.5f));
                        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override public void onAnimationUpdate(ValueAnimator a) {
                                try {
                                    lp.width = (int) a.getAnimatedValue();
                                    wm.updateViewLayout(root, lp);
                                } catch (Throwable ignored) {}
                            }
                        });
                        wa.start();
                    }
                }).start();
        } catch (Throwable ignored) {}
    }

    /** 对外接口：通知监听器调用 */
    public static void notify(String title, String text) {
        pendingNotifTitle = title;
        pendingNotifText = text;
        hasPendingNotif = true;
    }

    private void initChargeReceiver() {
        try {
            chargeReceiver = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent it) {
                    try {
                        if (it == null) return;
                        int level = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                        int scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                        int status = it.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                        boolean nowCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                            || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                        int pct = scale > 0 ? level * 100 / scale : 0;
                        updateBattery(pct, nowCharging);
                        if (nowCharging && !charging) {
                            charging = true;
                            showChargeAnim();
                        } else if (!nowCharging) {
                            charging = false;
                        }
                    } catch (Throwable ignored) {}
                }
            };
            registerReceiver(chargeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Throwable ignored) {}
    }

    private void updateBattery(int pct, boolean chg) {
        try {
            int color;
            String txt;
            if (chg) { color = 0xFF00FF88; txt = "⚡" + pct + "%"; }
            else {
                if (pct >= 80) color = 0xFF4CD964;
                else if (pct >= 50) color = 0xFF5AC8FA;
                else if (pct >= 20) color = 0xFFFFCC00;
                else color = 0xFFFF3B30;
                txt = pct + "%";
            }
            if (txtBattery != null) { txtBattery.setText(txt); txtBattery.setTextColor(color); }
        } catch (Throwable ignored) {}
    }

    private void showChargeAnim() {
        try {
            if (root != null) {
                root.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200)
                    .withEndAction(new Runnable() {
                        @Override public void run() {
                            root.animate().scaleX(1f).scaleY(1f).setDuration(300)
                                .setInterpolator(new OvershootInterpolator(1.5f)).start();
                        }
                    }).start();
            }
        } catch (Throwable ignored) {}
    }

    private void startBreathe() {
        try {
            if (root == null) return;
            if (breathe != null) breathe.cancel();
            breathe = ObjectAnimator.ofFloat(root, "alpha", 1f, 0.88f, 1f);
            breathe.setDuration(2200);
            breathe.setRepeatCount(ObjectAnimator.INFINITE);
            breathe.start();
        } catch (Throwable ignored) {}
    }

    private void stopBreathe() {
        try {
            if (breathe != null) { breathe.cancel(); breathe = null; }
            if (root != null) root.setAlpha(1f);
        } catch (Throwable ignored) {}
    }

    private void startRotate() {
        try {
            if (imgCover == null) return;
            if (coverRotate != null) coverRotate.cancel();
            coverRotate = ObjectAnimator.ofFloat(imgCover, "rotation", 0f, 360f);
            coverRotate.setDuration(18000);
            coverRotate.setRepeatCount(ObjectAnimator.INFINITE);
            coverRotate.setInterpolator(new android.view.animation.LinearInterpolator());
            coverRotate.start();
        } catch (Throwable ignored) {}
    }

    private void update() {
        try {
            ExoPlayer p = MusicService.getPlayer();
            MediaItem item = p != null ? p.getCurrentMediaItem() : null;
            long songId = 0;
            String title = "拾音", artist = "未播放";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }
            int idx = p != null ? p.getCurrentMediaItemIndex() : -1;
            List<Song> q = MusicService.sharedQueue;
            Song curSong = null;
            if (q != null && idx >= 0 && idx < q.size()) {
                curSong = q.get(idx);
                songId = curSong.id;
            }

            if (txtTitle != null && !title.equals(txtTitle.getText().toString()))
                txtTitle.setText(title);
            if (txtTitleBig != null && !title.equals(txtTitleBig.getText().toString()))
                txtTitleBig.setText(title);
            if (txtArtistBig != null && !artist.equals(txtArtistBig.getText().toString()))
                txtArtistBig.setText(artist);

            if (songId != lastSongId) {
                lastSongId = songId;
                PlayerActivity.currentLyric = "";
                lyricLines = new ArrayList<LyricsParser.Line>();
                lastLyric = "";
                try {
                    String name = WallpaperHelper.forSong(this, songId);
                    Bitmap bm = WallpaperHelper.loadSmall(this, name);
                    if (bm != null) {
                        if (imgCover != null) {
                            imgCover.setImageBitmap(bm);
                            imgCover.setClipToOutline(true);
                            if (Build.VERSION.SDK_INT >= 21) {
                                imgCover.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                    @Override public void getOutline(View v, android.graphics.Outline o) {
                                        o.setOval(0, 0, v.getWidth(), v.getHeight());
                                    }
                                });
                            }
                        }
                        if (imgCoverBig != null) {
                            imgCoverBig.setImageBitmap(bm);
                            imgCoverBig.setClipToOutline(true);
                            if (Build.VERSION.SDK_INT >= 21) {
                                imgCoverBig.setOutlineProvider(new android.view.ViewOutlineProvider() {
                                    @Override public void getOutline(View v, android.graphics.Outline o) {
                                        o.setOval(0, 0, v.getWidth(), v.getHeight());
                                    }
                                });
                            }
                        }
                        startRotate();
                    }
                } catch (Throwable ignored) {}
            }

            String lrc = PlayerActivity.currentLyric;
            if ((lrc == null || lrc.isEmpty()) && curSong != null) lrc = curSong.lyric;

            if (lrc == null || lrc.isEmpty()) {
                if (txtMiniLyric != null) txtMiniLyric.setText("未播放");
                for (TextView tv : lyricViews) if (tv != null) tv.setText("");
            } else {
                if (!lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                if (!lyricLines.isEmpty() && p != null) {
                    long pos = p.getCurrentPosition();
                    int li = LyricsParser.findIndex(lyricLines, pos);
                    if (txtMiniLyric != null && li >= 0 && li < lyricLines.size()) {
                        String cur = lyricLines.get(li).text;
                        if (!cur.equals(txtMiniLyric.getText().toString()))
                            txtMiniLyric.setText(cur);
                    }
                    if (expanded) {
                        for (int i = -2; i <= 2; i++) {
                            int ii = li + i;
                            String txt = (ii >= 0 && ii < lyricLines.size()) ? lyricLines.get(ii).text : "";
                            TextView tv = lyricViews[i + 2];
                            if (tv != null && !txt.equals(tv.getText().toString()))
                                tv.setText(txt);
                        }
                    }
                }
            }

            boolean playing = p != null && p.isPlaying();
            if (playing != lastPlaying) {
                lastPlaying = playing;
                String sym = playing ? "⏸" : "▶";
                if (btnPlay != null) btnPlay.setText(sym);
                if (btnPlayBig != null) btnPlayBig.setText(sym);
                if (playing) startBreathe(); else stopBreathe();
            }

            if (expanded && !dragging && seek != null && p != null) {
                long pos = p.getCurrentPosition();
                long dur = p.getDuration();
                if (dur > 0) {
                    seek.setMax(1000);
                    seek.setProgress((int)(pos * 1000 / dur));
                    if (txtTimeCur != null) txtTimeCur.setText(fmt((int) pos));
                    if (txtTimeTot != null) txtTimeTot.setText(fmt((int) dur));
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
        try { if (coverRotate != null) coverRotate.cancel(); } catch (Throwable ignored) {}
        try { if (breathe != null) breathe.cancel(); } catch (Throwable ignored) {}
        try { if (chargeReceiver != null) unregisterReceiver(chargeReceiver); } catch (Throwable ignored) {}
        h.removeCallbacksAndMessages(null);
        try { if (root != null && wm != null) wm.removeView(root); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
