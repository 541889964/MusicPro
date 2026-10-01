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
import java.util.Calendar;
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

    private View lifeBox, collapsedBox, expandedBox, notifSplit;
    private ImageView imgCover, imgCoverBig, imgNotifApp;
    private TextView txtClock, txtDate, txtLifeTip, txtBatteryLife;
    private TextView txtTitle, txtTitleBig, txtArtistBig, txtMiniLyric, txtBattery, txtBatteryBig;
    private TextView[] lyricViews = new TextView[5];
    private TextView txtTimeCur, txtTimeTot;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose;
    private TextView txtNotifTitle, txtNotifText, txtSplitTitle, txtSplitText;
    private SeekBar seek;
    private com.music.app.widget.RippleView ripple;
    private com.music.app.widget.ShineSweepView shine;
    private com.music.app.widget.ChargePulseView chargePulse;
    private com.music.app.widget.ParticleBreatheView breatheLayer;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, dragging = false;
    private boolean isPlayingMusic = false;
    private boolean notifShowing = false;

    private long lastSongId = -1;
    private String lastLyric = "";
    private boolean lastPlaying = false;
    private List<LyricsParser.Line> lyrics = new ArrayList<LyricsParser.Line>();
    private long lastNotifTime = 0;

    private ObjectAnimator coverRotate, breathe;
    private android.content.BroadcastReceiver chargeReceiver;
    private boolean charging = false;

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable t) { Log.e(TAG, "tick", t); }
            h.postDelayed(this, 300);
        }
    };
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            try { updateClock(); } catch (Throwable ignored) {}
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
        showLifeBox();

        root.setAlpha(0f);
        root.setScaleX(0.85f);
        root.setScaleY(0.85f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(400).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private void bindViews() {
        lifeBox = root.findViewById(R.id.lifeBox);
        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        notifSplit = root.findViewById(R.id.notifSplit);
        imgCover = root.findViewById(R.id.imgCover);
        imgCoverBig = root.findViewById(R.id.imgCoverBig);
        imgNotifApp = root.findViewById(R.id.imgNotifApp);
        txtClock = root.findViewById(R.id.txtClock);
        txtDate = root.findViewById(R.id.txtDate);
        txtLifeTip = root.findViewById(R.id.txtLifeTip);
        txtBatteryLife = root.findViewById(R.id.txtBatteryLife);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtTitleBig = root.findViewById(R.id.txtTitleBig);
        txtArtistBig = root.findViewById(R.id.txtArtistBig);
        txtMiniLyric = root.findViewById(R.id.txtMiniLyric);
        txtBattery = root.findViewById(R.id.txtBattery);
        txtBatteryBig = root.findViewById(R.id.txtBatteryBig);
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
        txtSplitTitle = root.findViewById(R.id.txtSplitTitle);
        txtSplitText = root.findViewById(R.id.txtSplitText);
        seek = root.findViewById(R.id.seek);
        try { ripple = root.findViewById(R.id.rippleLayer); } catch (Throwable ignored) {}
        try { shine = root.findViewById(R.id.shineLayer); } catch (Throwable ignored) {}
        try { chargePulse = root.findViewById(R.id.chargeLayer); } catch (Throwable ignored) {}
        try { breatheLayer = root.findViewById(R.id.breatheLayer); } catch (Throwable ignored) {}
    }

    private void bindClicks() {
        if (collapsedBox != null) collapsedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (!expanded) expand(); }
        });
        if (expandedBox != null) expandedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (expanded) collapse(); }
        });
        if (lifeBox != null) lifeBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                // 未播放时点击显示提示
                if (txtLifeTip != null) {
                    txtLifeTip.setText("播放音乐后切换");
                    h.postDelayed(new Runnable() {
                        @Override public void run() {
                            if (txtLifeTip != null) txtLifeTip.setText("生活区");
                        }
                    }, 1500);
                }
            }
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
            @Override public void onClick(final View v) {
                pressFeedback(v);
                try {
                    ExoPlayer p = MusicService.getPlayer();
                    if (p == null) return;
                    int idx = p.getCurrentMediaItemIndex();
                    p.seekTo(idx > 0 ? idx - 1 : 0, 0);
                    p.play();
                } catch (Throwable ignored) {}
            }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(final View v) {
                pressFeedback(v);
                try {
                    ExoPlayer p = MusicService.getPlayer();
                    if (p == null) return;
                    int total = p.getMediaItemCount();
                    int idx = p.getCurrentMediaItemIndex();
                    if (total <= 1) { p.seekTo(0, 0); p.play(); return; }
                    p.seekTo(idx < total - 1 ? idx + 1 : 0, 0);
                    p.play();
                } catch (Throwable ignored) {}
            }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });

        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    if (u) {
                        try {
                            ExoPlayer pl = MusicService.getPlayer();
                            if (pl != null) {
                                long dur = pl.getDuration();
                                if (dur > 0) pl.seekTo(dur * p / 1000);
                            }
                        } catch (Throwable ignored) {}
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

    /** 显示生活区 */
    private void showLifeBox() {
        if (isPlayingMusic) return;
        try {
            lifeBox.setVisibility(View.VISIBLE);
            collapsedBox.setVisibility(View.GONE);
            expandedBox.setVisibility(View.GONE);
        } catch (Throwable ignored) {}
        updateClock();
    }

    /** 显示音乐折叠态 */
    private void showMusicBox() {
        try {
            lifeBox.setVisibility(View.GONE);
            collapsedBox.setVisibility(View.VISIBLE);
        } catch (Throwable ignored) {}
    }

    private void updateClock() {
        try {
            Calendar c = Calendar.getInstance();
            String time = String.format(Locale.US, "%02d:%02d",
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE));
            if (txtClock != null) txtClock.setText(time);
            String[] days = {"周日","周一","周二","周三","周四","周五","周六"};
            String date = String.format(Locale.US, "%d月%d日 %s",
                c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH),
                days[c.get(Calendar.DAY_OF_WEEK) - 1]);
            if (txtDate != null) txtDate.setText(date);
            // 生活小贴士
            if (txtLifeTip != null) {
                String[] tips = {"今天也要开心", "喝口水吧", "慢慢来", "记得休息", "听听歌"};
                int idx = c.get(Calendar.MINUTE) % tips.length;
                String cur = txtLifeTip.getText().toString();
                if (!"播放音乐后切换".equals(cur)) txtLifeTip.setText(tips[idx]);
            }
        } catch (Throwable ignored) {}
    }

    /** 展开 */
    private void expand() {
        if (expanded || animating) return;
        expanded = true;
        animating = true;

        try {
        } catch (Throwable ignored) {}

        // 播放展开波纹（纯代码 Canvas 动画）
        try { if (ripple != null) ripple.startOnce(800); } catch (Throwable ignored) {}

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
                    expandedBox.animate()
                        .alpha(1f).scaleX(1f).scaleY(1f)
                        .setDuration(280)
                        .setInterpolator(new OvershootInterpolator(1.2f))
                        .withEndAction(new Runnable() {
                            @Override public void run() { animating = false; }
                        }).start();
                }
            });
            wa.start();

            if (btnPlayBig != null) {
                btnPlayBig.setScaleX(0.7f);
                btnPlayBig.setScaleY(0.7f);
                btnPlayBig.animate().scaleX(1f).scaleY(1f)
                    .setStartDelay(200).setDuration(400)
                    .setInterpolator(new OvershootInterpolator(1.8f)).start();
            }
        } catch (Throwable t) {
            Log.e(TAG, "expand", t);
            animating = false;
        }
    }

    /** 收起 */
    private void collapse() {
        if (!expanded || animating) return;
        expanded = false;
        animating = true;

        try {
        } catch (Throwable ignored) {}

        // 播放收起波纹
        try { if (ripple != null) ripple.startOnce(600); } catch (Throwable ignored) {}

        try {
            expandedBox.animate()
                .alpha(0f).scaleX(0.92f).scaleY(0.92f)
                .setDuration(180)
                .setInterpolator(new DecelerateInterpolator())
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
                                try {
                                    lp.height = cH();
                                    wm.updateViewLayout(root, lp);
                                } catch (Throwable ignored) {}
                                expandedBox.setVisibility(View.GONE);
                                if (isPlayingMusic) collapsedBox.setVisibility(View.VISIBLE);
                                else lifeBox.setVisibility(View.VISIBLE);
                                animating = false;
                            }
                        });
                        wa.start();
                    }
                }).start();
        } catch (Throwable t) {
            Log.e(TAG, "collapse", t);
            animating = false;
        }
    }

    /** ★ 通知分裂动画（右侧 1/3 冒出，5 秒后回收）*/
    public void onNewNotification() {
        try {
            lastNotifTime = System.currentTimeMillis();
            updateNotifUI();
            if (!notifShowing) {
                showNotifSplit();
            }
        } catch (Throwable ignored) {}
    }

    private void showNotifSplit() {
        if (notifShowing) return;
        notifShowing = true;

        // 扫光（纯代码）
        try { if (shine != null) shine.sweep(); } catch (Throwable ignored) {}

        try {
            // 更新内容
            String app = NotifListener.lastApp;
            String title = NotifListener.lastTitle;
            String text = NotifListener.lastText;
            if (txtSplitTitle != null) {
                txtSplitTitle.setText((app.isEmpty() ? "" : app + " · ") + title);
            }
            if (txtSplitText != null) txtSplitText.setText(text);

            notifSplit.setVisibility(View.VISIBLE);
            notifSplit.setTranslationX(screenW);
            notifSplit.setAlpha(0f);

            // 滑入
            notifSplit.animate()
                .translationX(0f)
                .alpha(1f)
                .setDuration(350)
                .setInterpolator(new OvershootInterpolator(1.1f))
                .start();

            // 5 秒后回收
            h.postDelayed(new Runnable() {
                @Override public void run() {
                    hideNotifSplit();
                }
            }, 5000);
        } catch (Throwable ignored) {}
    }

    private void hideNotifSplit() {
        try {
            notifSplit.animate()
                .translationX(screenW)
                .alpha(0f)
                .setDuration(280)
                .setInterpolator(new DecelerateInterpolator())
                .withEndAction(new Runnable() {
                    @Override public void run() {
                        notifSplit.setVisibility(View.GONE);
                        notifShowing = false;
                    }
                }).start();
        } catch (Throwable ignored) {
            notifShowing = false;
        }
    }

    private void updateNotifUI() {
        try {
            if (txtNotifTitle == null) return;
            String app = NotifListener.lastApp;
            String title = NotifListener.lastTitle;
            String text = NotifListener.lastText;
            boolean fresh = System.currentTimeMillis() - lastNotifTime < 5 * 60 * 1000;
            if (!fresh || (title.isEmpty() && text.isEmpty())) {
                txtNotifTitle.setText("暂无新通知");
                if (txtNotifText != null) txtNotifText.setText("");
                return;
            }
            txtNotifTitle.setText(app.isEmpty() ? title : (app + " · " + title));
            if (txtNotifText != null) txtNotifText.setText(text);
        } catch (Throwable ignored) {}
    }

    public void reloadConfig() {
        try {
            cfg = IslandConfig.load();
            if (!expanded) {
                lp.width = cW();
                lp.height = cH();
                wm.updateViewLayout(root, lp);
            }
        } catch (Throwable ignored) {}
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
            if (txtBatteryBig != null) { txtBatteryBig.setText(txt); txtBatteryBig.setTextColor(color); }
            if (txtBatteryLife != null) { txtBatteryLife.setText(txt); txtBatteryLife.setTextColor(color); }
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
            if (!expanded && isPlayingMusic) {
                expand();
                h.postDelayed(new Runnable() {
                    @Override public void run() { if (expanded) collapse(); }
                }, 2500);
            }
            h.postDelayed(new Runnable() {
                @Override public void run() {
                    if (chargePulse != null) chargePulse.stop();
                }
            }, 4000);
        } catch (Throwable ignored) {}
    }

    private void startBreathe() {
        try {
            if (root == null) return;
            if (breathe != null) breathe.cancel();
            breathe = ObjectAnimator.ofFloat(root, "alpha", 1f, 0.9f, 1f);
            breathe.setDuration(2200);
            breathe.setRepeatCount(ObjectAnimator.INFINITE);
            breathe.start();
            // 播放呼吸粒子
            if (framePlayer != null) {
                framePlayer.play("pulse", 30, 30, new com.music.app.widget.FramePlayer.OnEnd() {
                    @Override public void onEnd() {
                        if (lastPlaying && framePlayer != null) {
                            framePlayer.play("pulse", 30, 30, this);
                        }
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    private void stopBreathe() {
        try {
            if (breathe != null) { breathe.cancel(); breathe = null; }
            if (root != null) root.setAlpha(1f);
            if (breatheLayer != null) breatheLayer.stop();
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
            boolean nowPlaying = p != null && p.getCurrentMediaItem() != null;

            // ★ 切换生活区/音乐态
            if (nowPlaying != isPlayingMusic) {
                isPlayingMusic = nowPlaying;
                if (nowPlaying) {
                    if (!expanded) showMusicBox();
                } else {
                    if (!expanded) showLifeBox();
                }
            }

            if (!nowPlaying) {
                if (expanded) updateNotifUI();
                return;
            }

            MediaItem item = p.getCurrentMediaItem();
            long songId = 0;
            String title = "未播放", artist = "";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }

            int idx = p.getCurrentMediaItemIndex();
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
                lyrics = new ArrayList<LyricsParser.Line>();
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
            if (lrc == null || lrc.isEmpty()) {
                if (curSong != null) lrc = curSong.lyric;
            }

            if (lrc == null || lrc.isEmpty()) {
                if (txtMiniLyric != null) txtMiniLyric.setText("");
                for (TextView tv : lyricViews) if (tv != null) tv.setText("");
            } else {
                if (!lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyrics = LyricsParser.parse(lrc);
                }
                if (!lyrics.isEmpty()) {
                    long pos = p.getCurrentPosition();
                    int li = LyricsParser.findIndex(lyrics, pos);

                    if (txtMiniLyric != null && li >= 0 && li < lyrics.size()) {
                        String cur = lyrics.get(li).text;
                        if (!cur.equals(txtMiniLyric.getText().toString()))
                            txtMiniLyric.setText(cur);
                    }
                    if (expanded) {
                        for (int i = -2; i <= 2; i++) {
                            int ii = li + i;
                            String txt = (ii >= 0 && ii < lyrics.size())
                                ? lyrics.get(ii).text : "";
                            TextView tv = lyricViews[i + 2];
                            if (tv != null && !txt.equals(tv.getText().toString()))
                                tv.setText(txt);
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
                long pos = p.getCurrentPosition();
                long dur = p.getDuration();
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
