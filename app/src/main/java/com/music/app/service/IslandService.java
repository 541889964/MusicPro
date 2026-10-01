package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
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
    private TextView txtTitle, txtTitleBig, txtArtistBig, txtMiniLyric;
    private TextView[] lyricViews = new TextView[5];
    private TextView txtTimeCur, txtTimeTot;
    private TextView btnPlay, btnPlayBig, btnPrev, btnNext, btnClose;
    private SeekBar seek;

    private int screenW, screenH, statusBarH;
    private float density;
    private boolean expanded = false, animating = false, dragging = false;

    private long lastSongId = -1;
    private String lastLyric = "";
    private boolean lastPlaying = false;
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();

    private final Handler h = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try { update(); } catch (Throwable t) { Log.e(TAG, "tick", t); }
            h.postDelayed(this, 300);
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
            cfg = IslandConfig.load();
            measure();
            initView();
            h.post(tick);
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
        if (w < 20) w = 20; if (w > 95) w = 95;
        return (int)(screenW * w / 100f);
    }
    private int cH() {
        int h = cfg.collapsedH;
        if (h < 40) h = 40; if (h > 90) h = 90;
        return (int)(h * density);
    }
    private int eW() {
        int w = cfg.expandedW;
        if (w < 75) w = 75; if (w > 100) w = 100;
        return (int)(screenW * w / 100f);
    }
    private int eH() {
        int h = cfg.expandedH;
        if (h < 280) h = 280; if (h > 450) h = 450;
        return (int)(h * density);
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
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = statusBarH + (int)(6 * density);

        try { wm.addView(root, lp); }
        catch (Throwable t) { Log.e(TAG, "addView", t); stopSelf(); return; }

        collapsedBox = root.findViewById(R.id.collapsedBox);
        expandedBox = root.findViewById(R.id.expandedBox);
        imgCover = root.findViewById(R.id.imgCover);
        imgCoverBig = root.findViewById(R.id.imgCoverBig);
        txtTitle = root.findViewById(R.id.txtTitle);
        txtTitleBig = root.findViewById(R.id.txtTitleBig);
        txtArtistBig = root.findViewById(R.id.txtArtistBig);
        txtMiniLyric = root.findViewById(R.id.txtMiniLyric);
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

        collapsedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (!expanded) expand(); }
        });
        expandedBox.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { if (expanded) collapse(); }
        });

        View.OnClickListener playClick = new View.OnClickListener() {
            @Override public void onClick(View v) {
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
            @Override public void onClick(View v) {
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
            @Override public void onClick(View v) {
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

        root.setAlpha(0f);
        root.setScaleX(0.85f);
        root.setScaleY(0.85f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(300).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    /** ★ 流畅展开：只调 2 次 updateViewLayout + 内容 scale 动画 */
    private void expand() {
        if (expanded || animating) return;
        expanded = true;
        animating = true;

        // 1. 立即改窗口尺寸（只调一次）
        lp.width = eW();
        lp.height = eH();
        try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}

        // 2. 内容从 0.85 缩放到 1
        expandedBox.setVisibility(View.VISIBLE);
        expandedBox.setAlpha(0f);
        expandedBox.setScaleX(0.85f);
        expandedBox.setScaleY(0.85f);
        collapsedBox.setVisibility(View.GONE);

        expandedBox.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(320)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .withEndAction(new Runnable() {
                @Override public void run() {
                    animating = false;
                    update();
                }
            }).start();
    }

    /** ★ 流畅收起 */
    private void collapse() {
        if (!expanded || animating) return;
        expanded = false;
        animating = true;

        // 内容先缩回
        expandedBox.animate()
            .alpha(0f).scaleX(0.85f).scaleY(0.85f)
            .setDuration(200)
            .setInterpolator(new DecelerateInterpolator())
            .withEndAction(new Runnable() {
                @Override public void run() {
                    expandedBox.setVisibility(View.GONE);
                    collapsedBox.setVisibility(View.VISIBLE);
                    // 窗口缩回（只调一次）
                    lp.width = cW();
                    lp.height = cH();
                    try { wm.updateViewLayout(root, lp); } catch (Throwable ignored) {}
                    animating = false;
                }
            }).start();
    }

    public void reloadConfig() {
        cfg = IslandConfig.load();
        try {
            if (!expanded) {
                lp.width = cW();
                lp.height = cH();
                wm.updateViewLayout(root, lp);
            }
        } catch (Throwable ignored) {}
    }

    private void update() {
        try {
            ExoPlayer p = MusicService.getPlayer();
            if (p == null) return;

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

            // ★ 歌词：优先 PlayerActivity.currentLyric
            String lrc = PlayerActivity.currentLyric;
            if (lrc == null || lrc.isEmpty()) {
                if (curSong != null) lrc = curSong.lyric;
            }

            if (lrc != null && !lrc.isEmpty()) {
                if (!lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                if (!lyricLines.isEmpty()) {
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
                            String txt = (ii >= 0 && ii < lyricLines.size())
                                ? lyricLines.get(ii).text : "";
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
        } catch (Throwable t) { Log.e(TAG, "update", t); }
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override public void onDestroy() {
        instance = null;
        h.removeCallbacksAndMessages(null);
        try { if (root != null && wm != null) wm.removeView(root); }
        catch (Throwable ignored) {}
        super.onDestroy();
    }
}
