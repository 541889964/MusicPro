package com.music.app.service;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.R;
import com.music.app.model.Song;
import com.music.app.util.LyricsParser;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class IslandService extends Service {

    private WindowManager wm;
    private View island;
    private WindowManager.LayoutParams lp;
    private ExoPlayer player;
    private TextView tvTitle, tvArtist, tvExpTitle, tvExpArtist, tvLyric, tvCurrent, tvTotal;
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
    private String lastLyric = "";
    private List<LyricsParser.Line> lyricLines = new ArrayList<LyricsParser.Line>();

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
        tvExpTitle = island.findViewById(R.id.islandExpTitle);
        tvExpArtist = island.findViewById(R.id.islandExpArtist);
        tvLyric = island.findViewById(R.id.islandLyric);
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

        bindClick(btnPlay, new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });
        bindClick(btnPlayExp, new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });
        bindClick(btnPrev, new View.OnClickListener() {
            @Override public void onClick(View v) { prevSong(); }
        });
        bindClick(btnNext, new View.OnClickListener() {
            @Override public void onClick(View v) { nextSong(); }
        });
        bindClick(btnClose, new View.OnClickListener() {
            @Override public void onClick(View v) { stopSelf(); }
        });

        // 折叠态点击 → 展开
        if (collapsedRoot != null) {
            collapsedRoot.setClickable(true);
            collapsedRoot.setFocusable(true);
            collapsedRoot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    // 判断点击的是不是按钮（按钮会自己处理）
                    if (!expanded) expand();
                }
            });
        }
        // 展开态点击空白 → 收起
        if (expandedRoot != null) {
            expandedRoot.setClickable(true);
            expandedRoot.setFocusable(true);
            expandedRoot.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { if (expanded) collapse(); }
            });
        }

        if (progress != null) {
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
        }

        island.setAlpha(0f);
        island.setScaleX(0.4f);
        island.setScaleY(0.4f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(500).setInterpolator(new OvershootInterpolator(1.4f)).start();
    }

    private void bindClick(View v, final View.OnClickListener l) {
        if (v == null) return;
        v.setClickable(true);
        v.setFocusable(true);
        v.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                if (vib != null) { try { vib.vibrate(12); } catch (Throwable ignored) {} }
                view.animate().scaleX(0.88f).scaleY(0.88f).setDuration(70).start();
                view.postDelayed(new Runnable() {
                    @Override public void run() {
                        view.animate().scaleX(1f).scaleY(1f).setDuration(200)
                            .setInterpolator(new OvershootInterpolator(2.5f)).start();
                    }
                }, 70);
                l.onClick(view);
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
        if (player == null) return;
        int idx = player.getCurrentMediaItemIndex();
        if (idx > 0) {
            player.seekTo(idx - 1, 0);
        } else {
            player.seekTo(0, 0);
        }
        player.play();
    }

    private void nextSong() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player == null) return;
        int total = player.getMediaItemCount();
        int idx = player.getCurrentMediaItemIndex();
        if (idx < total - 1) {
            player.seekTo(idx + 1, 0);
        } else {
            player.seekTo(0, 0);
        }
        player.play();
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
        if (island == null || wm == null || expanded) return;
        lp.width = getW();
        lp.height = getH();
        try { wm.updateViewLayout(island, lp); } catch (Throwable ignored) {}
    }

    /** ★ iPhone 弹性展开 */
    private void expand() {
        if (expanded || animating) return;
        expanded = true;
        animating = true;

        final int startW = lp.width, startH = lp.height;
        final int targetW = (int)(screenW * 0.88f);
        final int targetH = (int)(205 * density);

        // 展开内容准备好
        expandedRoot.setVisibility(View.VISIBLE);
        expandedRoot.setAlpha(0f);

        // 宽高同时弹簧展开
        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
        ValueAnimator ha = ValueAnimator.ofInt(startH, targetH);
        wa.setDuration(480);
        ha.setDuration(480);
        wa.setInterpolator(new OvershootInterpolator(0.9f));
        ha.setInterpolator(new OvershootInterpolator(0.9f));

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

        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationStart(Animator a) {
                collapsedRoot.setVisibility(View.GONE);
            }
            @Override public void onAnimationEnd(Animator a) {
                animating = false;
                // 内容淡入
                expandedRoot.animate().alpha(1f).setDuration(200).start();
            }
        });
        wa.start(); ha.start();
    }

    /** ★ iPhone 弹性收起 */
    private void collapse() {
        if (!expanded || animating) return;
        expanded = false;
        animating = true;

        // 内容先淡出
        expandedRoot.animate().alpha(0f).setDuration(200).start();

        final int startW = lp.width, startH = lp.height;
        final int targetW = getW(), targetH = getH();

        ValueAnimator wa = ValueAnimator.ofInt(startW, targetW);
        ValueAnimator ha = ValueAnimator.ofInt(startH, targetH);
        wa.setDuration(400);
        ha.setDuration(400);
        wa.setInterpolator(new DecelerateInterpolator(1.5f));
        ha.setInterpolator(new DecelerateInterpolator(1.5f));

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

        ha.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator a) {
                expandedRoot.setVisibility(View.GONE);
                collapsedRoot.setVisibility(View.VISIBLE);
                collapsedRoot.setAlpha(0f);
                collapsedRoot.animate().alpha(1f).setDuration(200).start();
                animating = false;
            }
        });
        wa.start(); ha.start();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            update();
            h.postDelayed(this, 500);
        }
    };

    private void update() {
        if (player == null) player = MusicService.sharedPlayer;
        if (player == null) return;
        try {
            MediaItem item = player.getCurrentMediaItem();
            long songId = 0;
            String title = "正在播放";
            String artist = "MUSIC·Pro";
            if (item != null && item.mediaMetadata != null) {
                MediaMetadata md = item.mediaMetadata;
                if (md.title != null) title = md.title.toString();
                if (md.artist != null) artist = md.artist.toString();
            }
            // 拿到 id 用于匹配素材
            int idx = player.getCurrentMediaItemIndex();
            if (queue != null && idx >= 0 && idx < queue.size()) {
                songId = queue.get(idx).id;
            }

            if (!title.equals(lastTitle)) {
                if (tvTitle != null) tvTitle.setText(title);
                if (tvExpTitle != null) tvExpTitle.setText(title);
                lastTitle = title;
            }
            if (!artist.equals(lastArtist)) {
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
                // 切歌时也重新解析歌词
                lyricLines = new ArrayList<LyricsParser.Line>();
                lastLyric = "";
            }

            // 歌词：从 queue 里取缓存的 lyric
            if (queue != null && idx >= 0 && idx < queue.size()) {
                String lrc = queue.get(idx).lyric;
                if (lrc != null && !lrc.equals(lastLyric)) {
                    lastLyric = lrc;
                    lyricLines = LyricsParser.parse(lrc);
                }
                // 更新当前歌词行
                if (tvLyric != null && !lyricLines.isEmpty()) {
                    long pos = player.getCurrentPosition();
                    int li = LyricsParser.findIndex(lyricLines, pos);
                    if (li >= 0 && li < lyricLines.size()) {
                        String txt = lyricLines.get(li).text;
                        if (!txt.equals(tvLyric.getText().toString())) {
                            tvLyric.setText(txt);
                        }
                    }
                } else if (tvLyric != null) {
                    tvLyric.setText("");
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
