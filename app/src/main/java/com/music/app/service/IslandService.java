package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
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
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.FrameController;
import com.music.app.util.IslandConfig;
import com.music.app.util.LyricsParser;
import com.music.app.util.WallpaperHelper;
import com.music.app.widget.WaveformView;
import java.util.ArrayList;
import java.util.List;

public class IslandService extends Service {
    private static final String TAG = "Island";
    public static IslandService instance;

    private WindowManager wm;
    private View root;
    private WindowManager.LayoutParams lp;
    private IslandConfig cfg;

    private View collapsedBox, expandedBox;
    private ImageView imgCover, imgCoverBig;
    private TextView txtTitle, txtTitleBig, txtArtistBig;
    private TextView[] lyricViews = new TextView[5];
    private TextView txtTimeCur, txtTimeTot;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose, btnMode, btnFav;
    private SeekBar seek;
    private WaveformView wave;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, dragging = false;

    private FrameController fc;
    private long lastSongId = -1;
    private String lastTitle = "", lastArtist = "", lastLyric = "";
    private boolean lastPlaying = false;
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= 23 && !android.provider.Settings.canDrawOverlays(this)) {
            Log.e(TAG, "无悬浮窗权限");
            stopSelf();
            return;
        }
        try {
            instance = this;
            cfg = IslandConfig.load();
            measure();
            initView();
            startFrameLoop();
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

    private void initView() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        root = LayoutInflater.from(this).inflate(R.layout.view_island, null);

        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;

        lp = new WindowManager.LayoutParams(
            collapsedW(), collapsedH(), type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);

        wm.addView(root, lp);

        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        imgCover = root.findViewById(R.id.imgCover);
        imgCoverBig = root.findViewById(R.id.imgCoverBig);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtTitleBig = root.findViewById(R.id.txtTitleBig);
        txtArtistBig = root.findViewById(R.id.txtArtistBig);
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
        btnMode = root.findViewById(R.id.btnMode);
        btnFav = root.findViewById(R.id.btnFav);
        seek = root.findViewById(R.id.seek);
        wave = root.findViewById(R.id.wave);

        // 点击展开 / 空白收起
        collapsedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (!expanded) expand(); }
        });
        expandedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (expanded) collapse(); }
        });

        // 按钮
        bindClick(btnPlay, new Runnable() { @Override public void run() {
            MusicService.toggle(getApplicationContext()); }});
        bindClick(btnPlayBig, new Runnable() { @Override public void run() {
            MusicService.toggle(getApplicationContext()); }});
        bindClick(btnPrev, new Runnable() { @Override public void run() {
            try {
                androidx.media3.exoplayer.ExoPlayer p = MusicService.getPlayer();
                if (p != null && p.hasPreviousMediaItem()) {
                    p.seekToPreviousMediaItem();
                    p.play();
                } else if (p != null) {
                    p.seekTo(0, 0);
                    p.play();
                }
            } catch (Throwable ignored) {}
        }});
        bindClick(btnNext, new Runnable() { @Override public void run() {
            try {
                androidx.media3.exoplayer.ExoPlayer p = MusicService.getPlayer();
                if (p != null && p.hasNextMediaItem()) {
                    p.seekToNextMediaItem();
                    p.play();
                } else if (p != null) {
                    p.seekTo(0, 0);
                    p.play();
                }
            } catch (Throwable ignored) {}
        }});
        bindClick(btnClose, new Runnable() { @Override public void run() { stopSelf(); }});
        bindClick(btnFav, new Runnable() { @Override public void run() {
            toast("已加入喜欢"); }});
        bindClick(btnMode, new Runnable() { @Override public void run() {
            toast("单曲循环"); }});

        // 进度
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

        // 入场
        root.setAlpha(0f);
        root.setScaleX(0.85f); root.setScaleY(0.85f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(350).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private int collapsedW() {
        int w = cfg.collapsedW;
        if (w < 20) w = 20; if (w > 95) w = 95;
        return (int)(screenW * w / 100f);
    }
    private int collapsedH() {
        int h = cfg.collapsedH;
        if (h < 30) h = 30; if (h > 90) h = 90;
        return (int)(h * density);
    }
    private int expandedW() {
        int w = cfg.expandedW;
        // ★ 最小 75%，保证按钮不被挤
        if (w < 75) w = 75;
        if (w > 100) w = 100;
        return (int)(screenW * w / 100f);
    }
    private int expandedH() {
        int h = cfg.expandedH;
        // ★ 最小 260dp，保证顶部+歌词+进度+按钮全部可见
        if (h < 260) h = 260;
        if (h > 450) h = 450;
        return (int)(h * density);
    }

    private void bindClick(View v, final Runnable action) {
        if (v == null) return;
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                try { action.run(); } catch (Throwable t) { Log.e(TAG, "click", t); }
                try {
                    view.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70).start();
                    view.postDelayed(new Runnable() {
                        @Override public void run() {
                            view.animate().scaleX(1f).scaleY(1f).setDuration(180)
                                .setInterpolator(new OvershootInterpolator(2f)).start();
                        }
                    }, 70);
                } catch (Throwable ignored) {}
            }
        });
    }

    private void expand() {
        if (expanded || animating) return;
        expanded = true; animating = true;

        expandedBox.setVisibility(View.VISIBLE);
        expandedBox.setAlpha(0f);

        ValueAnimator wa = ValueAnimator.ofInt(lp.width, expandedW());
        ValueAnimator ha = ValueAnimator.ofInt(lp.height, expandedH());
        wa.setDuration(420); ha.setDuration(420);
        wa.setInterpolator(new OvershootInterpolator(1.0f));
        ha.setInterpolator(new OvershootInterpolator(1.0f));
        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.width = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.height = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator a) { collapsedBox.setVisibility(View.GONE); }
            @Override public void onAnimationEnd(Animator a) {
                expandedBox.animate().alpha(1f).setDuration(200).start();
                animating = false;
            }
        });
        wa.start(); ha.start();
    }

    private void collapse() {
        if (!expanded || animating) return;
        expanded = false; animating = true;
        expandedBox.animate().alpha(0f).setDuration(150).start();

        ValueAnimator wa = ValueAnimator.ofInt(lp.width, collapsedW());
        ValueAnimator ha = ValueAnimator.ofInt(lp.height, collapsedH());
        wa.setDuration(340); ha.setDuration(340);
        wa.setInterpolator(new DecelerateInterpolator());
        ha.setInterpolator(new DecelerateInterpolator());
        wa.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.width = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                lp.height = (int) a.getAnimatedValue();
                try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
            }
        });
        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                expandedBox.setVisibility(View.GONE);
                collapsedBox.setVisibility(View.VISIBLE);
                animating = false;
            }
        });
        wa.start(); ha.start();
    }

    /** 从设置界面调用 */
    public void reloadConfig() {
        cfg = IslandConfig.load();
        try {
            if (fc != null) fc.setFps(cfg.fps);
        } catch (Throwable ignored) {}
        try {
            if (!expanded) {
                lp.width = collapsedW();
                lp.height = collapsedH();
                wm.updateViewLayout(root, lp);
            }
        } catch (Throwable ignored) {}
    }

    private void startFrameLoop() {
        if (fc != null) fc.stop();
        fc = new FrameController(cfg.fps, new FrameController.Tick() {
            @Override public void onFrame() {
                try {
                    if (wave != null) {
                        wave.setPlaying(lastPlaying);
                        wave.step();
                    }
                    update();
                } catch (Throwable t) { Log.e(TAG, "frame", t); }
            }
        });
        fc.start();
    }

    private void update() {
        try {
            MediaItem item = MusicService.getCurrentItem();
            long songId = 0;
            String title = "未播放", artist = "";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }
            List<Song> q = MusicService.sharedQueue;
            int idx = MusicService.getIndex();
            if (q != null && idx >= 0 && idx < q.size()) songId = q.get(idx).id;

            if (!title.equals(lastTitle)) {
                if (txtTitle != null) txtTitle.setText(title);
                if (txtTitleBig != null) txtTitleBig.setText(title);
                lastTitle = title;
            }
            if (!artist.equals(lastArtist)) {
                if (txtArtistBig != null) txtArtistBig.setText(artist);
                lastArtist = artist;
            }

            if (songId != lastSongId) {
                lastSongId = songId;
                try {
                    String name = WallpaperHelper.forSong(this, songId);
                    Bitmap bm = WallpaperHelper.loadSmall(this, name);
                    if (bm != null) {
                        if (imgCover != null) imgCover.setImageBitmap(bm);
                        if (imgCoverBig != null) imgCoverBig.setImageBitmap(bm);
                    }
                } catch (Throwable ignored) {}
                lyricLines = new ArrayList<LyricsParser.Line>();
                lastLyric = "";
            }

            if (expanded && q != null && idx >= 0 && idx < q.size()) {
                String lrc = q.get(idx).lyric;
                if (lrc != null && !lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                if (!lyricLines.isEmpty()) {
                    long pos = MusicService.getPos();
                    int li = LyricsParser.findIndex(lyricLines, pos);
                    for (int i = -2; i <= 2; i++) {
                        int ii = li + i;
                        String text = (ii >= 0 && ii < lyricLines.size())
                            ? lyricLines.get(ii).text : "";
                        TextView tv = lyricViews[i + 2];
                        if (tv != null && !text.equals(tv.getText().toString()))
                            tv.setText(text);
                    }
                } else {
                    for (TextView tv : lyricViews) {
                        if (tv != null) tv.setText("");
                    }
                }
            }

            boolean playing = MusicService.isPlaying();
            if (playing != lastPlaying) {
                lastPlaying = playing;
                String sym = playing ? "⏸" : "▶";
                if (btnPlay != null) btnPlay.setText(sym);
                if (btnPlayBig != null) btnPlayBig.setText(sym);
                if (wave != null) wave.setPlaying(playing);
            }

            if (expanded && !dragging && seek != null) {
                long pos = MusicService.getPos(), dur = MusicService.getDur();
                if (dur > 0) {
                    seek.setMax(1000);
                    seek.setProgress((int)(pos * 1000 / dur));
                    if (txtTimeCur != null) txtTimeCur.setText(fmt((int) pos));
                    if (txtTimeTot != null) txtTimeTot.setText(fmt((int) dur));
                }
            }
        } catch (Throwable ignored) {}
    }

    private void toast(String s) {
        try {
            android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {}
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override public void onDestroy() {
        instance = null;
        if (fc != null) fc.stop();
        try {
            if (root != null && wm != null) wm.removeView(root);
        } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
