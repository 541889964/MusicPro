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
import com.music.app.util.IslandConfig;
import com.music.app.util.LyricsParser;
import com.music.app.util.WallpaperHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class IslandService extends Service {
    private static final String TAG = "Island";
    public static IslandService instance;

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams lp;
    private IslandConfig cfg;

    private View collapsedBox, expandedBox, playInfo, lifeInfo;
    private ImageView imgCover, imgCoverBig;
    private TextView txtTitle, txtTitleBig, txtArtistBig, txtMiniLyric;
    private TextView txtBattery, txtBatteryBig, txtTime, txtDate;
    private TextView[] lyricViews = new TextView[5];
    private TextView txtTimeCur, txtTimeTot;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose;
    private TextView txtNotifTitle, txtNotifText;
    private SeekBar seek;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, dragging = false;
    private boolean hasPlaying = false;

    private long lastSongId = -1;
    private String lastLyric = "";
    private boolean lastPlaying = false;
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();
    private long lastNotifTime = 0;

    private ObjectAnimator coverRotate, breathe;
    private android.content.BroadcastReceiver chargeReceiver;
    private boolean charging = false;

    private View notifView;
    private WindowManager.LayoutParams notifLp;
    private boolean notifShowing = false;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable t) { Log.e(TAG, "tick", t); }
            h.postDelayed(this, 300);
        }
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            h.postDelayed(this, 1000);
        }
    };

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        try {
            instance = this;
            cfg = IslandConfig.load();
            measure();
            initView();
            initChargeReceiver();
            h.post(tick);
            h.post(clockTick);
        } catch (Throwable t) {
            Log.e(TAG, "init fail", t);
            stopSelf();
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override public void onTaskRemoved(Intent rootIntent) {
        try {
            Intent r = new Intent(getApplicationContext(), IslandService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(r);
            else startService(r);
        } catch (Throwable ignored) {}
        super.onTaskRemoved(rootIntent);
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
        int w = cfg.collapsedW;
        if (w < 10) w = 10; if (w > 100) w = 100;
        return (int)(screenW * w / 100f);
    }
    private int cH() {
        int h = cfg.collapsedH;
        if (h < 30) h = 30; if (h > 150) h = 150;
        return (int)(h * density);
    }
    private int eW() {
        int w = cfg.expandedW;
        if (w < 30) w = 30; if (w > 110) w = 110;
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

        updateClock();
    }

    private void bindViews() {
        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        playInfo = root.findViewById(R.id.playInfo);
        lifeInfo = root.findViewById(R.id.lifeInfo);
        imgCover = root.findViewById(R.id.imgCover);
        imgCoverBig = root.findViewById(R.id.imgCoverBig);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtTitleBig = root.findViewById(R.id.txtTitleBig);
        txtArtistBig = root.findViewById(R.id.txtArtistBig);
        txtMiniLyric = root.findViewById(R.id.txtMiniLyric);
        txtBattery = root.findViewById(R.id.txtBattery);
        txtBatteryBig = root.findViewById(R.id.txtBatteryBig);
        txtTime = root.findViewById(R.id.txtTime);
        txtDate = root.findViewById(R.id.txtDate);
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
        txtNotifTitle = root.findViewById(R.id.txtNotifTitle);
        txtNotifText = root.findViewById(R.id.txtNotifText);
        seek = root.findViewById(R.id.seek);
    }

    private void bindClicks() {
        if (collapsedBox != null) collapsedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (!expanded && hasPlaying) expand(); }
        });
        if (expandedBox != null) expandedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (expanded) collapse(); }
        });
        View.OnClickListener playClick = new View.OnClickListener() {
            @Override public void onClick(final View v) {
                pressFeedback(v);
                try {
                    ExoPlayer p = MusicService.getPlayer();
                    if (p == null) return;
                    if (p.isPlaying()) p.pause(); else p.play();
                } catch (Throwable ignored) {}
            }
        };
        if (btnPlay != null) btnPlay.setOnClickListener(playClick);
        if (btnPlayBig != null) btnPlayBig.setOnClickListener(playClick);
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) { pressFeedback(v); MusicService.prev(getApplicationContext()); }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) { pressFeedback(v); MusicService.next(getApplicationContext()); }
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

    private void pressFeedback(View v) {
        try {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70).start();
            v.postDelayed(new Runnable() {
                @Override public void run() {
                    v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                        .setInterpolator(new OvershootInterpolator(2.5f)).start();
                }
            }, 70);
        } catch (Throwable ignored) {}
    }

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
                    try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                    catch (Throwable ignored) {}
                }
            });
            wa.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    expandedBox.animate().alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(280).setInterpolator(new OvershootInterpolator(1.2f))
                        .withEndAction(new Runnable() { @Override public void run() { animating = false; } }).start();
                }
            });
            wa.start();
            if (btnPlayBig != null) {
                btnPlayBig.setScaleX(0.7f); btnPlayBig.setScaleY(0.7f);
                btnPlayBig.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(200).setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
            }
        } catch (Throwable t) { animating = false; }
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
                        ValueAnimator wa = ValueAnimator.ofInt(startW, cW());
                        wa.setDuration(280);
                        wa.setInterpolator(new DecelerateInterpolator(1.5f));
                        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override public void onAnimationUpdate(ValueAnimator a) {
                                try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                                catch (Throwable ignored) {}
                            }
                        });
                        wa.addListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a) {
                                try { lp.height = cH(); wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
                                expandedBox.setVisibility(View.GONE);
                                collapsedBox.setVisibility(View.VISIBLE);
                                collapsedBox.setAlpha(0f);
                                collapsedBox.animate().alpha(1f).setDuration(180)
                                    .withEndAction(new Runnable() { @Override public void run() { animating = false; } }).start();
                            }
                        });
                        wa.start();
                    }
                }).start();
        } catch (Throwable t) { animating = false; }
    }

    public void reloadConfig() {
        try {
            cfg = IslandConfig.load();
            if (!expanded) {
                lp.width = cW(); lp.height = cH();
                wm.updateViewLayout(root, lp);
            }
        } catch (Throwable ignored) {}
    }

    private void updateClock() {
        try {
            if (txtTime != null)
                txtTime.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            if (txtDate != null) {
                String[] wd = {"周日","周一","周二","周三","周四","周五","周六"};
                java.util.Calendar c = java.util.Calendar.getInstance();
                String w = wd[c.get(java.util.Calendar.DAY_OF_WEEK) - 1];
                String d = new SimpleDateFormat("M月d日", Locale.getDefault()).format(new Date());
                txtDate.setText(w + " · " + d);
            }
        } catch (Throwable ignored) {}
    }

    public void onNewNotification() {
        try {
            lastNotifTime = System.currentTimeMillis();
            if (hasPlaying && !expanded && !notifShowing) showNotifSplit();
        } catch (Throwable ignored) {}
    }

    public void testNotifSplit() {
        try {
            if (notifShowing) return;
            if (NotifListener.lastTitle == null || NotifListener.lastTitle.isEmpty()) {
                NotifListener.lastApp = "测试";
                NotifListener.lastTitle = "这是一条测试通知";
                NotifListener.lastText = "分裂动画效果演示";
            }
            showNotifSplit();
        } catch (Throwable ignored) {}
    }

    private void showNotifSplit() {
        try {
            notifShowing = true;
            notifView = LayoutInflater.from(this).inflate(R.layout.view_notif_split, null);
            ImageView icon = notifView.findViewById(R.id.nImgIcon);
            TextView app = notifView.findViewById(R.id.nTxtApp);
            TextView title = notifView.findViewById(R.id.nTxtTitle);
            if (app != null) app.setText(NotifListener.lastApp);
            if (title != null) title.setText(NotifListener.lastTitle.isEmpty()
                ? NotifListener.lastText : NotifListener.lastTitle);
            if (icon != null) icon.setImageResource(R.mipmap.ic_launcher);

            int type = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

            final int notifW = screenW / 3;
            int notifH = cH();
            notifLp = new WindowManager.LayoutParams(0, notifH, type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
            notifLp.gravity = Gravity.TOP | Gravity.END;
            notifLp.y = statusBarH + (int)(6 * density);
            notifLp.x = (int)(8 * density);

            try { wm.addView(notifView, notifLp); }
            catch (Throwable t) { notifShowing = false; return; }

            final int origW = lp.width;
            final int newW = Math.max(origW - notifW - (int)(12 * density), origW / 2);

            ValueAnimator wa = ValueAnimator.ofInt(0, notifW);
            wa.setDuration(320);
            wa.setInterpolator(new OvershootInterpolator(1.15f));
            wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try { notifLp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(notifView, notifLp); }
                    catch (Throwable ignored) {}
                }
            });
            wa.start();

            ValueAnimator cwa = ValueAnimator.ofInt(origW, newW);
            cwa.setDuration(320);
            cwa.setInterpolator(new OvershootInterpolator(1.15f));
            cwa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                    catch (Throwable ignored) {}
                }
            });
            cwa.start();

            h.postDelayed(new Runnable() {
                @Override public void run() { hideNotifSplit(); }
            }, 5000);
        } catch (Throwable t) {
            Log.e(TAG, "showNotifSplit", t);
            notifShowing = false;
        }
    }

    private void hideNotifSplit() {
        if (!notifShowing || notifView == null) return;
        try {
            final int startW = notifLp.width;
            ValueAnimator wa = ValueAnimator.ofInt(startW, 0);
            wa.setDuration(280);
            wa.setInterpolator(new DecelerateInterpolator(1.5f));
            wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try { notifLp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(notifView, notifLp); }
                    catch (Throwable ignored) {}
                }
            });
            wa.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    try { wm.removeView(notifView); } catch (Throwable ignored) {}
                    notifView = null;
                    notifShowing = false;
                }
            });
            wa.start();

            final int startMainW = lp.width;
            ValueAnimator cwa = ValueAnimator.ofInt(startMainW, cW());
            cwa.setDuration(280);
            cwa.setInterpolator(new DecelerateInterpolator(1.5f));
            cwa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override public void onAnimationUpdate(ValueAnimator a) {
                    try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                    catch (Throwable ignored) {}
                }
            });
            cwa.start();
        } catch (Throwable t) {
            try { wm.removeView(notifView); } catch (Throwable ignored) {}
            notifView = null;
            notifShowing = false;
        }
    }

    private void updateNotifUI() {
        try {
            if (txtNotifTitle == null) return;
            boolean fresh = System.currentTimeMillis() - lastNotifTime < 5 * 60 * 1000;
            if (!fresh || (NotifListener.lastTitle.isEmpty() && NotifListener.lastText.isEmpty())) {
                txtNotifTitle.setText("暂无新通知");
                if (txtNotifText != null) txtNotifText.setText("");
                return;
            }
            String head = NotifListener.lastApp.isEmpty()
                ? NotifListener.lastTitle
                : (NotifListener.lastApp + " · " + NotifListener.lastTitle);
            txtNotifTitle.setText(head);
            if (txtNotifText != null) txtNotifText.setText(NotifListener.lastText);
        } catch (Throwable ignored) {}
    }

    private void initChargeReceiver() {
        try {
            chargeReceiver = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent it) {
                    try {
                        if (it == null) return;
                        int lvl = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                        int scale = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                        int status = it.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                        boolean nowCharging = status == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                            || status == android.os.BatteryManager.BATTERY_STATUS_FULL;
                        int pct = scale > 0 ? lvl * 100 / scale : 0;
                        updateBattery(pct, nowCharging);
                        if (nowCharging && !charging) { charging = true; showChargeAnim(); }
                        else if (!nowCharging) charging = false;
                    } catch (Throwable ignored) {}
                }
            };
            registerReceiver(chargeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        } catch (Throwable ignored) {}
    }

    private void updateBattery(int pct, boolean chg) {
        try {
            int color; String txt;
            if (chg) { color = 0xFF00FF88; txt = "⚡" + pct + "%"; }
            else {
                if (pct >= 80) color = 0xFF4CD964;
                else if (pct >= 50) color = 0xFF5AC8FA;
                else if (pct >= 20) color = 0xFFFFCC00;
                else color = 0xFFFF3B30;
                txt = pct + "%";
            }
            if (txtBattery != null) { txtBattery.setText(txt); txtBattery.setTextColor(color); }
            if (txtBatteryBig != null) { txtBatteryBig.setText(txt); txtBatteryBig.setTextColor(color); }
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
            if (p == null) {
                if (hasPlaying) { hasPlaying = false; switchToLife(); stopBreathe(); }
                return;
            }
            boolean anyPlaying = p.getMediaItemCount() > 0 && p.getCurrentMediaItem() != null;
            if (anyPlaying != hasPlaying) {
                hasPlaying = anyPlaying;
                if (hasPlaying) switchToPlay(); else switchToLife();
            }
            if (!anyPlaying) return;

            MediaItem item = p.getCurrentMediaItem();
            long songId = 0;
            String title = "", artist = "";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }
            int idx = p.getCurrentMediaItemIndex();
            List<Song> q = MusicService.sharedQueue;
            Song curSong = null;
            if (q != null && idx >= 0 && idx < q.size()) { curSong = q.get(idx); songId = curSong.id; }

            if (txtTitle != null && !title.equals(txtTitle.getText().toString())) txtTitle.setText(title);
            if (txtTitleBig != null && !title.equals(txtTitleBig.getText().toString())) txtTitleBig.setText(title);
            if (txtArtistBig != null && !artist.equals(txtArtistBig.getText().toString())) txtArtistBig.setText(artist);

            if (songId != lastSongId) {
                lastSongId = songId;
                PlayerActivity.currentLyric = "";
                lyricLines = new ArrayList<LyricsParser.Line>();
                lastLyric = "";
                try {
                    String name = WallpaperHelper.forSong(this, songId);
                    Bitmap bm = WallpaperHelper.loadSmall(this, name);
                    if (bm != null) {
                        if (imgCover != null) imgCover.setImageBitmap(bm);
                        if (imgCoverBig != null) imgCoverBig.setImageBitmap(bm);
                        startRotate();
                    }
                } catch (Throwable ignored) {}
            }

            String lrc = PlayerActivity.currentLyric;
            if (lrc == null || lrc.isEmpty()) if (curSong != null) lrc = curSong.lyric;

            if (lrc == null || lrc.isEmpty()) {
                if (txtMiniLyric != null) txtMiniLyric.setText("");
                for (TextView tv : lyricViews) if (tv != null) tv.setText("");
            } else {
                if (!lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                if (!lyricLines.isEmpty()) {
                    long pos = p.getCurrentPosition();
                    int li = LyricsParser.findIndex(lyricLines, pos);
                    if (txtMiniLyric != null && li >= 0 && li < lyricLines.size()) {
                        String cur = lyricLines.get(li).text;
                        if (!cur.equals(txtMiniLyric.getText().toString())) txtMiniLyric.setText(cur);
                    }
                    if (expanded) {
                        for (int i = -2; i <= 2; i++) {
                            int ii = li + i;
                            String txt = (ii >= 0 && ii < lyricLines.size()) ? lyricLines.get(ii).text : "";
                            TextView tv = lyricViews[i + 2];
                            if (tv != null && !txt.equals(tv.getText().toString())) tv.setText(txt);
                        }
                    }
                }
            }

            boolean playing = p.isPlaying();
            if (playing != lastPlaying) {
                lastPlaying = playing;
                String sym = playing ? "⏸" : "▶";
                if (btnPlay != null) btnPlay.setText(sym);
                if (btnPlayBig != null) btnPlayBig.setText(sym);
                if (playing) startBreathe(); else stopBreathe();
            }

            if (expanded && !dragging && seek != null) {
                long pos = p.getCurrentPosition(), dur = p.getDuration();
                if (dur > 0) {
                    seek.setMax(1000);
                    seek.setProgress((int)(pos * 1000 / dur));
                    if (txtTimeCur != null) txtTimeCur.setText(fmt((int) pos));
                    if (txtTimeTot != null) txtTimeTot.setText(fmt((int) dur));
                }
            }

            if (expanded) updateNotifUI();
        } catch (Throwable t) { Log.e(TAG, "update", t); }
    }

    private void switchToLife() {
        if (hasPlaying) return;
        try {
            if (playInfo != null) playInfo.setVisibility(View.GONE);
            if (lifeInfo != null) lifeInfo.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {}
    }

    private void switchToPlay() {
        if (!hasPlaying) return;
        try {
            if (playInfo != null) playInfo.setVisibility(View.VISIBLE);
            if (lifeInfo != null) lifeInfo.setVisibility(View.GONE);
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
        if (notifView != null) { try { wm.removeView(notifView); } catch (Throwable ignored) {} notifView = null; }
        h.removeCallbacksAndMessages(null);
        try { if (root != null && wm != null) wm.removeView(root); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
