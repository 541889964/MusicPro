package com.music.app.service;

import android.animation.*;
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
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import com.music.app.PlayerActivity;
import com.music.app.R;
import com.music.app.util.Prefs;
import com.music.app.util.RandomAssets;
import com.music.app.util.WallpaperHelper;
import com.music.app.widget.ChargePulseView;
import com.music.app.widget.ParticleBreatheView;
import com.music.app.widget.RippleView;
import com.music.app.widget.ShineSweepView;
import java.util.Calendar;
import java.util.Locale;

public class IslandService extends Service {
    private static final String TAG = "Island";
    public static IslandService instance;

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams lp;

    private View lifeBox, collapsedBox, expandedBox, notifSplit, glowLayer;
    private TextView txtClock, txtDate, txtTitle, txtMiniLyric, txtBattery, txtBatteryBig;
    private TextView txtTimeCur, txtTimeTot, txtNotifTitle, txtNotifText, txtSplitTitle, txtSplitText;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose;
    private ImageView ivCover, ivCoverBig;
    private SeekBar seek;
    private RippleView ripple;
    private ShineSweepView shine;
    private ChargePulseView chargePulse;
    private ParticleBreatheView breatheLayer;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, notifShowing = false;
    private boolean isPlaying = false;

    private ObjectAnimator rotate, breathe;
    private android.content.BroadcastReceiver chargeRec;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable t) { Log.e(TAG, "tick", t); }
            h.postDelayed(this, 500);
        }
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            try { updateClock(); } catch (Throwable ignored) {}
            h.postDelayed(this, 1000);
        }
    };

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public int onStartCommand(Intent i, int f, int s) {
        startForegroundSafe();
        return START_STICKY;
    }

    private void startForegroundSafe() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                String CH = "island_fg";
                android.app.NotificationManager nm =
                    (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CH) == null) {
                    android.app.NotificationChannel c = new android.app.NotificationChannel(
                        CH, "灵动岛", android.app.NotificationManager.IMPORTANCE_MIN);
                    c.setShowBadge(false);
                    nm.createNotificationChannel(c);
                }
                android.app.Notification n =
                    new androidx.core.app.NotificationCompat.Builder(this, CH)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle("拾音运行中")
                        .setPriority(androidx.core.app.NotificationCompat.PRIORITY_MIN)
                        .setOngoing(true).build();
                startForeground(0x9527, n);
            }
        } catch (Throwable ignored) {}
    }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        try {
            instance = this;
            measure();
            initView();
            initCharge();
            h.post(tick);
            h.post(clockTick);
        } catch (Throwable t) {
            Log.e(TAG, "init", t);
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
        int w = Prefs.cW(this);
        if (w < 20) w = 20; if (w > 100) w = 100;
        return (int)(screenW * w / 100f);
    }
    private int cH() {
        int hh = Prefs.cH(this);
        if (hh < 30) hh = 30; if (hh > 120) hh = 120;
        return (int)(hh * density);
    }
    private int eW() {
        int w = Prefs.eW(this);
        if (w < 50) w = 50; if (w > 100) w = 100;
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

        lifeBox = root.findViewById(R.id.lifeBox);
        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        notifSplit = root.findViewById(R.id.notifSplit);
        glowLayer = root.findViewById(R.id.glowLayer);
        txtClock = root.findViewById(R.id.txtClock);
        txtDate = root.findViewById(R.id.txtDate);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtMiniLyric = root.findViewById(R.id.txtMiniLyric);
        txtBattery = root.findViewById(R.id.txtBattery);
        txtBatteryBig = root.findViewById(R.id.txtBatteryBig);
        txtTimeCur = root.findViewById(R.id.txtTimeCur);
        txtTimeTot = root.findViewById(R.id.txtTimeTot);
        txtNotifTitle = root.findViewById(R.id.txtNotifTitle);
        txtNotifText = root.findViewById(R.id.txtNotifText);
        txtSplitTitle = root.findViewById(R.id.txtSplitTitle);
        txtSplitText = root.findViewById(R.id.txtSplitText);
        btnPlay = root.findViewById(R.id.btnPlay);
        btnPlayBig = root.findViewById(R.id.btnPlayBig);
        btnPrev = root.findViewById(R.id.btnPrev);
        btnNext = root.findViewById(R.id.btnNext);
        btnClose = root.findViewById(R.id.btnClose);
        ivCover = root.findViewById(R.id.ivCover);
        ivCoverBig = root.findViewById(R.id.ivCoverBig);
        seek = root.findViewById(R.id.seek);
        ripple = root.findViewById(R.id.rippleLayer);
        shine = root.findViewById(R.id.shineLayer);
        chargePulse = root.findViewById(R.id.chargeLayer);
        breatheLayer = root.findViewById(R.id.breatheLayer);

        // 图标：用 assets 里的随机图当封面
        try {
            Bitmap cv = RandomAssets.cover(this);
            if (cv != null) {
                if (ivCover != null) ivCover.setImageBitmap(cv);
                if (ivCoverBig != null) ivCoverBig.setImageBitmap(cv);
            }
        } catch (Throwable ignored) {}

        bindClicks();
        showLife();

        root.setAlpha(0f);
        root.setScaleX(0.85f);
        root.setScaleY(0.85f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private void bindClicks() {
        if (lifeBox != null) lifeBox.setOnClickListener(v -> { if (!expanded) expand(); });
        if (collapsedBox != null) collapsedBox.setOnClickListener(v -> { if (!expanded) expand(); });
        if (expandedBox != null) expandedBox.setOnClickListener(v -> { if (expanded) collapse(); });

        View.OnClickListener playClick = v -> {
            press(v);
            haptic();
            try {
                if (PlayerActivity.player != null) {
                    if (PlayerActivity.player.isPlaying()) PlayerActivity.player.pause();
                    else PlayerActivity.player.play();
                }
            } catch (Throwable ignored) {}
        };
        if (btnPlay != null) btnPlay.setOnClickListener(playClick);
        if (btnPlayBig != null) btnPlayBig.setOnClickListener(playClick);

        if (btnPrev != null) btnPrev.setOnClickListener(v -> {
            press(v); haptic();
            try {
                if (PlayerActivity.player != null && PlayerActivity.currentIndex > 0) {
                    PlayerActivity.player.seekTo(PlayerActivity.currentIndex - 1, 0);
                    PlayerActivity.player.play();
                }
            } catch (Throwable ignored) {}
        });
        if (btnNext != null) btnNext.setOnClickListener(v -> {
            press(v); haptic();
            try {
                if (PlayerActivity.player != null && PlayerActivity.currentIndex < PlayerActivity.queue.size() - 1) {
                    PlayerActivity.player.seekTo(PlayerActivity.currentIndex + 1, 0);
                    PlayerActivity.player.play();
                }
            } catch (Throwable ignored) {}
        });
        if (btnClose != null) btnClose.setOnClickListener(v -> { haptic(); stopSelf(); });

        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    if (u && PlayerActivity.player != null) {
                        long d = PlayerActivity.player.getDuration();
                        if (d > 0) PlayerActivity.player.seekTo(d * p / 1000);
                    }
                }
                public void onStartTrackingTouch(SeekBar sb) {}
                public void onStopTrackingTouch(SeekBar sb) {}
            });
        }
    }

    private void haptic() {
        try { root.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK); }
        catch (Throwable ignored) {}
    }

    private void press(View v) {
        try {
            v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70)
                .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                    .setInterpolator(new OvershootInterpolator(2.5f)).start()).start();
        } catch (Throwable ignored) {}
    }

    private void showLife() {
        try {
            if (lifeBox != null) lifeBox.setVisibility(View.VISIBLE);
            if (collapsedBox != null) collapsedBox.setVisibility(View.GONE);
            if (expandedBox != null) expandedBox.setVisibility(View.GONE);
        } catch (Throwable ignored) {}
    }

    private void showMusic() {
        try {
            if (lifeBox != null) lifeBox.setVisibility(View.GONE);
            if (collapsedBox != null) collapsedBox.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {}
    }

    private void updateClock() {
        try {
            Calendar c = Calendar.getInstance();
            if (txtClock != null) {
                txtClock.setText(String.format(Locale.US, "%02d:%02d",
                    c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE)));
            }
            String[] d = {"周日","周一","周二","周三","周四","周五","周六"};
            if (txtDate != null) {
                txtDate.setText(String.format(Locale.US, "%d月%d日 %s",
                    c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                    d[c.get(Calendar.DAY_OF_WEEK) - 1]));
            }
        } catch (Throwable ignored) {}
    }

    // ★ 展开：触感 + 内容错峰入场
    private void expand() {
        if (expanded || animating) return;
        expanded = true; animating = true;
        haptic();
        try { if (ripple != null) ripple.startOnce(800); } catch (Throwable ignored) {}
        try {
            final int sw = lp.width;
            final int tw = eW();
            lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
            if (expandedBox != null) {
                expandedBox.setVisibility(View.VISIBLE);
                expandedBox.setAlpha(0f);
                expandedBox.setScaleX(0.94f);
                expandedBox.setScaleY(0.94f);
            }
            if (collapsedBox != null) collapsedBox.setVisibility(View.GONE);
            if (lifeBox != null) lifeBox.setVisibility(View.GONE);

            staggerIn();

            ValueAnimator va = ValueAnimator.ofInt(sw, tw);
            va.setDuration(340);
            va.setInterpolator(new OvershootInterpolator(1.08f));
            va.addUpdateListener(a -> {
                try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                catch (Throwable ignored) {}
            });
            va.addListener(new AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(Animator a) {
                    if (expandedBox != null) {
                        expandedBox.animate().alpha(1f).scaleX(1f).scaleY(1f)
                            .setDuration(260).setInterpolator(new OvershootInterpolator(1.2f))
                            .withEndAction(() -> animating = false).start();
                    } else animating = false;
                }
            });
            va.start();
        } catch (Throwable t) { Log.e(TAG, "expand", t); animating = false; }
    }

    // ★ 展开内容错峰入场
    private void staggerIn() {
        try {
            final View[] kids = { ivCoverBig, txtTitleBig(), txtBatteryBig, seek, btnPlayBig };
            for (int i = 0; i < kids.length; i++) {
                View v = kids[i];
                if (v == null) continue;
                v.setAlpha(0f);
                v.setTranslationY(14f);
                v.animate().alpha(1f).translationY(0f)
                    .setStartDelay(i * 55L)
                    .setDuration(240)
                    .start();
            }
        } catch (Throwable ignored) {}
    }

    private View txtTitleBig() {
        try { return root.findViewById(R.id.txtTitleBig); } catch (Throwable t) { return null; }
    }

    // ★ 收起：AnticipateInterpolator 吸回 + 触感
    private void collapse() {
        if (!expanded || animating) return;
        expanded = false; animating = true;
        haptic();
        try { if (ripple != null) ripple.startOnce(600); } catch (Throwable ignored) {}
        try {
            if (expandedBox != null) {
                expandedBox.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f)
                    .setDuration(180)
                    .withEndAction(() -> {
                        final int sw = lp.width;
                        final int tw = cW();
                        ValueAnimator va = ValueAnimator.ofInt(sw, tw);
                        va.setDuration(300);
                        va.setInterpolator(new AnticipateInterpolator(1.3f));
                        va.addUpdateListener(a -> {
                            try { lp.width = (int) a.getAnimatedValue(); wm.updateViewLayout(root, lp); }
                            catch (Throwable ignored) {}
                        });
                        va.addListener(new AnimatorListenerAdapter() {
                            @Override public void onAnimationEnd(Animator a) {
                                try { lp.height = cH(); wm.updateViewLayout(root, lp); }
                                catch (Throwable ignored) {}
                                if (expandedBox != null) expandedBox.setVisibility(View.GONE);
                                if (isPlaying) { if (collapsedBox != null) collapsedBox.setVisibility(View.VISIBLE); }
                                else { if (lifeBox != null) lifeBox.setVisibility(View.VISIBLE); }
                                animating = false;
                            }
                        });
                        va.start();
                    }).start();
            } else animating = false;
        } catch (Throwable t) { Log.e(TAG, "collapse", t); animating = false; }
    }

    public void onNotif() {
        try { if (!notifShowing) showNotif(); } catch (Throwable ignored) {}
    }

    // ★ 通知分裂：加轻微旋转
    private void showNotif() {
        if (notifShowing) return;
        notifShowing = true;
        try { if (shine != null) shine.sweep(); } catch (Throwable ignored) {}
        try {
            String app = NotifListener.lastApp;
            String title = NotifListener.lastTitle;
            String text = NotifListener.lastText;
            String head = app.isEmpty() ? title : (app + " · " + title);
            if (txtSplitTitle != null) txtSplitTitle.setText(head);
            if (txtSplitText != null) txtSplitText.setText(text);
            if (txtNotifTitle != null) txtNotifTitle.setText(head);
            if (txtNotifText != null) txtNotifText.setText(text);
            if (notifSplit != null) {
                notifSplit.setVisibility(View.VISIBLE);
                notifSplit.setTranslationX(screenW);
                notifSplit.setRotation(-3f);
                notifSplit.setAlpha(0f);
                notifSplit.animate()
                    .translationX(0f).rotation(0f).alpha(1f)
                    .setDuration(380)
                    .setInterpolator(new OvershootInterpolator(1.15f))
                    .start();
            }
            h.postDelayed(this::hideNotif, 5000);
        } catch (Throwable ignored) {}
    }

    private void hideNotif() {
        try {
            if (notifSplit != null) {
                notifSplit.animate().translationX(screenW).alpha(0f).rotation(3f)
                    .setDuration(280)
                    .withEndAction(() -> {
                        if (notifSplit != null) {
                            notifSplit.setVisibility(View.GONE);
                            notifSplit.setRotation(0f);
                        }
                        notifShowing = false;
                    }).start();
            } else notifShowing = false;
        } catch (Throwable ignored) { notifShowing = false; }
    }

    private void initCharge() {
        try {
            chargeRec = new android.content.BroadcastReceiver() {
                @Override public void onReceive(android.content.Context c, Intent it) {
                    try {
                        if (it == null) return;
                        int lvl = it.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, 0);
                        int sc = it.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, 100);
                        int st = it.getIntExtra(android.os.BatteryManager.EXTRA_STATUS, -1);
                        boolean chg = st == android.os.BatteryManager.BATTERY_STATUS_CHARGING
                            || st == android.os.BatteryManager.BATTERY_STATUS_FULL;
                        int pct = sc > 0 ? lvl * 100 / sc : 0;
                        updateBattery(pct, chg);
                        // ★ 充电动画接上
                        if (chg) { if (chargePulse != null) chargePulse.start(); }
                        else { if (chargePulse != null) chargePulse.stop(); }
                    } catch (Throwable ignored) {}
                }
            };
            registerReceiver(chargeRec, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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

    public void reloadConfig() {
        try {
            lp.width = expanded ? eW() : cW();
            lp.height = expanded ? WindowManager.LayoutParams.WRAP_CONTENT : cH();
            wm.updateViewLayout(root, lp);
        } catch (Throwable ignored) {}
    }

    // ★ update：封面用 PlayerActivity.sharedCover 复用，不额外分配
    private void update() {
        try {
            boolean nowPlaying = PlayerActivity.player != null
                && PlayerActivity.player.getCurrentMediaItem() != null
                && PlayerActivity.player.getMediaItemCount() > 0;
            if (nowPlaying != isPlaying) {
                isPlaying = nowPlaying;
                if (!expanded) {
                    if (nowPlaying) showMusic(); else showLife();
                }
                if (nowPlaying) startBreathe(); else stopBreathe();
            }
            if (!nowPlaying) return;

            // 复用播放页封面（不分配新 Bitmap）
            Bitmap shared = PlayerActivity.sharedCover;
            if (shared != null && !shared.isRecycled()) {
                if (ivCover != null) ivCover.setImageBitmap(shared);
                if (ivCoverBig != null) ivCoverBig.setImageBitmap(shared);
                startRotate();
            } else if (ivCover != null && ivCover.getDrawable() == null) {
                Bitmap bm = WallpaperHelper.generateCircleCover(200);
                if (bm != null) {
                    ivCover.setImageBitmap(bm);
                    if (ivCoverBig != null) ivCoverBig.setImageBitmap(bm);
                    startRotate();
                }
            }

            try {
                MediaItem item = PlayerActivity.player.getCurrentMediaItem();
                if (item != null && item.mediaMetadata != null) {
                    CharSequence t = item.mediaMetadata.title;
                    if (t != null) {
                        if (txtTitle != null && !t.toString().contentEquals(txtTitle.getText()))
                            txtTitle.setText(t);
                        View tb = txtTitleBig();
                        if (tb instanceof TextView) {
                            TextView tv = (TextView) tb;
                            if (!t.toString().contentEquals(tv.getText())) tv.setText(t);
                        }
                    }
                }
            } catch (Throwable ignored) {}

            String lrc = PlayerActivity.currentLyric;
            if (lrc != null && !lrc.isEmpty() && txtMiniLyric != null) {
                String[] lines = lrc.split("\n");
                if (lines.length > 0) {
                    long pos = PlayerActivity.player.getCurrentPosition();
                    int li = (int) (pos / 3000) % lines.length;
                    String line = lines[li].replaceAll("\\[.*?\\]", "").trim();
                    if (!line.isEmpty() && !line.equals(txtMiniLyric.getText().toString())) {
                        txtMiniLyric.setText(line);
                    }
                }
            }
            boolean playing = PlayerActivity.player.isPlaying();
            String sym = playing ? "⏸" : "▶";
            if (btnPlay != null && !sym.contentEquals(btnPlay.getText())) btnPlay.setText(sym);
            if (btnPlayBig != null && !sym.contentEquals(btnPlayBig.getText())) btnPlayBig.setText(sym);

            if (expanded && seek != null) {
                long pos = PlayerActivity.player.getCurrentPosition();
                long dur = PlayerActivity.player.getDuration();
                if (dur > 0) {
                    seek.setMax(1000);
                    seek.setProgress((int) (pos * 1000 / dur));
                    if (txtTimeCur != null) txtTimeCur.setText(fmt((int) pos));
                    if (txtTimeTot != null) txtTimeTot.setText(fmt((int) dur));
                }
            }
        } catch (Throwable t) { Log.e(TAG, "update", t); }
    }

    private void startRotate() {
        try {
            if (ivCover == null) return;
            if (rotate != null) return;
            rotate = ObjectAnimator.ofFloat(ivCover, "rotation", 0f, 360f);
            rotate.setDuration(18000);
            rotate.setRepeatCount(ObjectAnimator.INFINITE);
            rotate.setInterpolator(new LinearInterpolator());
            rotate.start();
        } catch (Throwable ignored) {}
    }

    // ★ 呼吸动 glowLayer，不动 root（避免和 shine 打架）
    private void startBreathe() {
        try {
            View target = glowLayer != null ? glowLayer : root;
            if (target == null) return;
            if (breathe != null) return;
            breathe = ObjectAnimator.ofFloat(target, "alpha", 0.35f, 0.80f, 0.35f);
            breathe.setDuration(2400);
            breathe.setRepeatCount(ObjectAnimator.INFINITE);
            breathe.start();
            if (breatheLayer != null) breatheLayer.start();
        } catch (Throwable ignored) {}
    }

    private void stopBreathe() {
        try {
            if (breathe != null) { breathe.cancel(); breathe = null; }
            if (glowLayer != null) glowLayer.setAlpha(0.55f);
            if (root != null) root.setAlpha(1f);
            if (breatheLayer != null) breatheLayer.stop();
        } catch (Throwable ignored) {}
    }

    private String fmt(int ms) { int s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60); }

    @Override public void onDestroy() {
        instance = null;
        try { stopForeground(true); } catch (Throwable ignored) {}
        try { if (rotate != null) rotate.cancel(); } catch (Throwable ignored) {}
        try { if (breathe != null) breathe.cancel(); } catch (Throwable ignored) {}
        try { if (chargeRec != null) unregisterReceiver(chargeRec); } catch (Throwable ignored) {}
        h.removeCallbacksAndMessages(null);
        try { if (root != null && wm != null) wm.removeView(root); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
