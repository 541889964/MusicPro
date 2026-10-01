package com.music.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.music.app.model.Song;
import com.music.app.service.MusicService;
import com.music.app.util.LyricsParser;
import com.music.app.util.NeteaseApi;
import com.music.app.util.NiceToast;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    public static ExoPlayer player;
    public static List<Song> queue = new ArrayList<Song>();
    public static String currentLyric = "";
    public static String currentTitle = "";
    public static String currentArtist = "";
    public static int currentIndex = 0;
    private static boolean listenerBound = false;

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageView ivCover, ivBg;
    private View ivCoverWrap, glowBehindCover;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose, btnDownload, btnFav, btnShare;
    private android.widget.ScrollView lyricsScroll;
    private LinearLayout lyricsContainer;
    private TextView tvLyricsEmpty;

    private List<LyricsParser.Line> lyrics = new ArrayList<LyricsParser.Line>();
    private List<TextView> lyricViews = new ArrayList<TextView>();
    private int currentLine = -1;
    private ObjectAnimator coverRotate, glowPulse;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging = false;
    private boolean destroyed = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_player);
        } catch (Throwable t) {
            NiceToast.show(this, "布局加载失败: " + t.getMessage());
            finish();
            return;
        }

        try {
            // 绑定所有 view（每个都 try-catch）
            safeBind();

            // 检查队列
            if (queue == null || queue.isEmpty()) {
                NiceToast.show(this, "播放列表为空");
                finish();
                return;
            }

            int startIdx = 0;
            try { startIdx = getIntent().getIntExtra("index", 0); } catch (Throwable ignored) {}
            if (startIdx < 0 || startIdx >= queue.size()) startIdx = 0;
            currentIndex = startIdx;

            // 初始化 player
            try {
                player = MusicService.ensurePlayer(this);
                if (player == null) {
                    NiceToast.show(this, "播放器初始化失败");
                    finish();
                    return;
                }
                bindListenerOnce();
            } catch (Throwable t) {
                NiceToast.show(this, "初始化失败: " + t.getMessage());
                finish();
                return;
            }

            // 开始播放
            playAt(currentIndex);

            // 启动灵动岛（需要悬浮窗权限）
            try {
                if (android.os.Build.VERSION.SDK_INT < 23
                    || android.provider.Settings.canDrawOverlays(this)) {
                    startService(new Intent(this, com.music.app.service.IslandService.class));
                }
            } catch (Throwable ignored) {}

            // 按钮事件
            setupButtons();

            // 进度条
            if (seek != null) {
                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                        if (fu && tvCurrent != null) tvCurrent.setText(fmt(p));
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                    @Override public void onStopTrackingTouch(SeekBar sb) {
                        try { if (player != null) player.seekTo(sb.getProgress()); } catch (Throwable ignored) {}
                        dragging = false;
                    }
                });
            }

            // 光晕动画
            try {
                if (glowBehindCover != null) {
                    glowPulse = ObjectAnimator.ofFloat(glowBehindCover, "alpha", 0.35f, 0.85f);
                    glowPulse.setDuration(1800);
                    glowPulse.setRepeatCount(ObjectAnimator.INFINITE);
                    glowPulse.setRepeatMode(ObjectAnimator.REVERSE);
                    glowPulse.start();
                }
            } catch (Throwable ignored) {}

            // 进度定时
            h.post(progressTick);

            // 入场动画
            View card = findViewById(R.id.playerCard);
            if (card != null) {
                card.setAlpha(0f); card.setTranslationY(60f);
                card.animate().alpha(1f).translationY(0f).setDuration(650)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
            if (ivCoverWrap != null) {
                ivCoverWrap.setAlpha(0f); ivCoverWrap.setScaleX(0.7f); ivCoverWrap.setScaleY(0.7f);
                ivCoverWrap.animate().alpha(1f).scaleX(1f).scaleY(1f)
                    .setStartDelay(150).setDuration(700)
                    .setInterpolator(new OvershootInterpolator(1.1f)).start();
            }
        } catch (Throwable t) {
            android.util.Log.e("Music", "PlayerActivity fail", t);
            NiceToast.show(this, "播放页异常: " + t.getMessage());
            finish();
        }
    }

    private void safeBind() {
        try { ivBg = findViewById(R.id.ivPlayerBg); } catch (Throwable ignored) {}
        try { tvTitle = findViewById(R.id.tvPlayerTitle); } catch (Throwable ignored) {}
        try { tvArtist = findViewById(R.id.tvPlayerArtist); } catch (Throwable ignored) {}
        try { tvCurrent = findViewById(R.id.tvCurrent); } catch (Throwable ignored) {}
        try { tvTotal = findViewById(R.id.tvTotal); } catch (Throwable ignored) {}
        try { seek = findViewById(R.id.seekBar); } catch (Throwable ignored) {}
        try { ivCover = findViewById(R.id.ivCover); } catch (Throwable ignored) {}
        try { ivCoverWrap = findViewById(R.id.ivCoverWrap); } catch (Throwable ignored) {}
        try { glowBehindCover = findViewById(R.id.glowBehindCover); } catch (Throwable ignored) {}
        try { btnPlay = findViewById(R.id.btnPlay); } catch (Throwable ignored) {}
        try { btnPrev = findViewById(R.id.btnPrev); } catch (Throwable ignored) {}
        try { btnNext = findViewById(R.id.btnNext); } catch (Throwable ignored) {}
        try { btnClose = findViewById(R.id.btnClose); } catch (Throwable ignored) {}
        try { btnDownload = findViewById(R.id.btnDownload); } catch (Throwable ignored) {}
        try { btnFav = findViewById(R.id.btnFav); } catch (Throwable ignored) {}
        try { btnShare = findViewById(R.id.btnShare); } catch (Throwable ignored) {}
        try { lyricsScroll = findViewById(R.id.lyricsScroll); } catch (Throwable ignored) {}
        try { lyricsContainer = findViewById(R.id.lyricsContainer); } catch (Throwable ignored) {}
        try { tvLyricsEmpty = findViewById(R.id.tvLyricsEmpty); } catch (Throwable ignored) {}

        try {
            if (ivBg != null) {
                Bitmap bm = WallpaperHelper.loadCurrent(this);
                if (bm != null) ivBg.setImageBitmap(bm);
            }
        } catch (Throwable ignored) {}
    }

    private void setupButtons() {
        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); togglePlay(); }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                if (currentIndex > 0) playAt(currentIndex - 1);
            }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                if (currentIndex < queue.size() - 1) playAt(currentIndex + 1);
            }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        if (btnDownload != null) btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); downloadCurrent(); }
        });
        if (btnFav != null) btnFav.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                NiceToast.love(PlayerActivity.this, "已加入喜欢");
            }
        });
        if (btnShare != null) btnShare.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                bounce(v);
                NiceToast.show(PlayerActivity.this, "分享开发中");
            }
        });
    }

    private void bindListenerOnce() {
        if (listenerBound || player == null) return;
        listenerBound = true;
        try {
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED && !destroyed) {
                        if (currentIndex < queue.size() - 1) playAt(currentIndex + 1);
                    }
                }
                @Override public void onMediaItemTransition(MediaItem item, int reason) {
                    if (item == null || destroyed) return;
                    try {
                        currentIndex = player.getCurrentMediaItemIndex();
                        updateTitle(item);
                        startCoverRotate();
                    } catch (Throwable ignored) {}
                }
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    if (destroyed) return;
                    try {
                        if (isPlaying) startCoverRotate(); else stopCoverRotate();
                        updatePlayIcon();
                    } catch (Throwable ignored) {}
                }
            });
        } catch (Throwable ignored) {}
    }

    private void updateTitle(MediaItem item) {
        try {
            if (item == null || item.mediaMetadata == null) return;
            MediaMetadata md = item.mediaMetadata;
            if (md.title != null && tvTitle != null) tvTitle.setText(md.title);
            if (md.artist != null && tvArtist != null) tvArtist.setText(md.artist);
            int idx = player.getCurrentMediaItemIndex();
            if (idx >= 0 && idx < queue.size()) {
                Song song = queue.get(idx);
                String name = WallpaperHelper.forSong(this, song.id);
                Bitmap coverBm = WallpaperHelper.load(this, name);
                if (coverBm != null && ivCover != null) ivCover.setImageBitmap(coverBm);
                if (song.lyric != null && !song.lyric.isEmpty()) buildLyricsView(song.lyric);
                else if (song.isOnline) loadLyrics(song.id, idx);
                else loadLocalLyrics(song);
            }
        } catch (Throwable ignored) {}
    }

    private void playAt(int idx) {
        if (player == null) return;
        if (idx < 0 || idx >= queue.size()) return;
        currentIndex = idx;
        final Song song = queue.get(idx);

        try {
            if (tvTitle != null) tvTitle.setText(song.title);
            if (tvArtist != null) tvArtist.setText(song.artist);
            if (tvTotal != null) tvTotal.setText(song.getDurationText());
        } catch (Throwable ignored) {}

        // 封面
        try {
            if (ivCover != null) {
                ivCover.setAlpha(0f);
                ivCover.animate().alpha(1f).setDuration(400).start();
                String sucaiName = WallpaperHelper.forSong(this, song.id);
                Bitmap coverBm = WallpaperHelper.load(this, sucaiName);
                if (coverBm != null) ivCover.setImageBitmap(coverBm);
                else if (song.cover != null && !song.cover.isEmpty())
                    Glide.with(this).load(song.cover).into(ivCover);
                else ivCover.setImageResource(R.mipmap.ic_launcher);
            }
        } catch (Throwable ignored) {}

        // 歌词：切歌先清空静态变量
        try {
            currentLyric = "";
            currentTitle = song.title;
            currentArtist = song.artist;
            if (song.lyric != null && !song.lyric.isEmpty()) buildLyricsView(song.lyric);
            else if (song.isOnline) loadLyrics(song.id, idx);
            else loadLocalLyrics(song);
        } catch (Throwable ignored) {}

        // ★ 构建整队并播放
        try {
            List<MediaItem> items = new ArrayList<MediaItem>();
            for (int i = 0; i < queue.size(); i++) {
                Song sg = queue.get(i);
                String uri;
                if (sg.isOnline) {
                    if (sg.onlineUrl == null || sg.onlineUrl.isEmpty()) {
                        sg.onlineUrl = "http://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
                    }
                    uri = sg.onlineUrl;
                } else {
                    uri = "file://" + sg.path;
                }
                MediaMetadata md = new MediaMetadata.Builder()
                    .setTitle(sg.title != null ? sg.title : "")
                    .setArtist(sg.artist != null ? sg.artist : "")
                    .build();
                items.add(new MediaItem.Builder().setUri(uri).setMediaMetadata(md).build());
            }
            MusicService.playItems(items, idx);
            updatePlayIcon();
        } catch (Throwable t) {
            android.util.Log.e("Music", "playAt fail", t);
            NiceToast.show(this, "播放失败: " + t.getMessage());
        }
    }

    private void loadLyrics(final long songId, final int idx) {
        try {
            if (lyricsContainer != null) lyricsContainer.removeAllViews();
            if (tvLyricsEmpty != null) {
                tvLyricsEmpty.setVisibility(View.VISIBLE);
                tvLyricsEmpty.setText("♪ 歌词加载中…");
            }
            lyrics = new ArrayList<LyricsParser.Line>();
            lyricViews = new ArrayList<TextView>();
            currentLine = -1;
            NeteaseApi.getLyrics(songId, new NeteaseApi.OnLyrics() {
                @Override public void onResult(String lrc) {
                    if (destroyed) return;
                    if (lrc == null || lrc.isEmpty()) {
                        if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
                        return;
                    }
                    if (idx >= 0 && idx < queue.size()) queue.get(idx).lyric = lrc;
                    buildLyricsView(lrc);
                }
            });
        } catch (Throwable ignored) {}
    }

    private void loadLocalLyrics(Song song) {
        try {
            if (lyricsContainer != null) lyricsContainer.removeAllViews();
            lyrics = new ArrayList<LyricsParser.Line>();
            lyricViews = new ArrayList<TextView>();
            currentLine = -1;

            // 1. 先找同目录同名 .lrc
            String lrcPath = song.path;
            int dot = lrcPath.lastIndexOf('.');
            if (dot > 0) lrcPath = lrcPath.substring(0, dot) + ".lrc";
            java.io.File f = new java.io.File(lrcPath);
            if (f.exists()) {
                java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
                r.close();
                song.lyric = sb.toString();
                buildLyricsView(song.lyric);
                return;
            }

            // 2. ★ 本地音乐联网搜歌词
            if (tvLyricsEmpty != null) {
                tvLyricsEmpty.setVisibility(View.VISIBLE);
                tvLyricsEmpty.setText("♪ 联网搜歌词中…");
            }
            String keyword = song.title;
            if (song.artist != null && !song.artist.isEmpty()
                && !song.artist.equals("未知歌手")
                && !song.artist.equals("本地音乐")) {
                keyword = song.title + " " + song.artist;
            }
            final String kw = keyword;
            final Song fs = song;
            NeteaseApi.search(kw, new NeteaseApi.OnSearch() {
                @Override public void onResult(java.util.List<Song> songs) {
                    if (destroyed) return;
                    if (songs == null || songs.isEmpty()) {
                        if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
                        return;
                    }
                    final long sid = songs.get(0).id;
                    NeteaseApi.getLyrics(sid, new NeteaseApi.OnLyrics() {
                        @Override public void onResult(String lrc) {
                            if (destroyed) return;
                            if (lrc == null || lrc.isEmpty()) {
                                if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
                                return;
                            }
                            fs.lyric = lrc;
                            buildLyricsView(lrc);
                        }
                    });
                }
            });
        } catch (Throwable ignored) {}
    }

    private void buildLyricsView(String lrc) {
        try {
            currentLyric = lrc;
            if (queue != null && currentIndex >= 0 && currentIndex < queue.size()) {
                Song cs = queue.get(currentIndex);
                if (cs != null) {
                    currentTitle = cs.title;
                    currentArtist = cs.artist;
                }
            }
        } catch (Throwable ignored) {}
        try {
            lyrics = LyricsParser.parse(lrc);
            if (lyrics.isEmpty()) {
                if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
                return;
            }
            if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.GONE);
            if (lyricsContainer == null) return;
            lyricsContainer.removeAllViews();
            lyricViews.clear();
            float d = getResources().getDisplayMetrics().density;
            int padH = (int)(32 * d); int padV = (int)(9 * d);
            for (int i = 0; i < lyrics.size(); i++) {
                TextView tv = new TextView(this);
                tv.setText(lyrics.get(i).text);
                tv.setTextColor(0x66FFFFFF);
                tv.setTextSize(15);
                tv.setPadding(padH, padV, padH, padV);
                tv.setGravity(android.view.Gravity.CENTER);
                tv.setLineSpacing(0, 1.3f);
                lyricsContainer.addView(tv);
                lyricViews.add(tv);
            }
            View top = new View(this);
            top.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(180 * d)));
            View bottom = new View(this);
            bottom.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(180 * d)));
            lyricsContainer.addView(top, 0);
            lyricsContainer.addView(bottom);
        } catch (Throwable ignored) {}
    }

    private void updateLyricsScroll(long pos) {
        if (lyrics.isEmpty() || lyricViews.isEmpty()) return;
        try {
            int idx = LyricsParser.findIndex(lyrics, pos);
            if (idx == currentLine || idx < 0 || idx >= lyricViews.size()) return;
            int old = currentLine;
            currentLine = idx;
            TextView cur = lyricViews.get(currentLine);
            cur.setTextColor(0xFFFF6B9D);
            cur.setTextSize(17);
            if (old >= 0 && old < lyricViews.size()) {
                TextView o = lyricViews.get(old);
                o.setTextColor(0x66FFFFFF); o.setTextSize(15);
            }
            if (lyricsScroll != null) {
                final View v = lyricViews.get(currentLine);
                lyricsScroll.post(new Runnable() {
                    @Override public void run() {
                        try {
                            int target = v.getTop() - lyricsScroll.getHeight() / 2 + v.getHeight() / 2;
                            lyricsScroll.smoothScrollTo(0, Math.max(0, target));
                        } catch (Throwable ignored) {}
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            try {
                if (player != null && seek != null) {
                    long pos = player.getCurrentPosition();
                    long dur = player.getDuration();
                    if (dur > 0) {
                        if (!dragging) { seek.setMax((int) dur); seek.setProgress((int) pos); }
                        if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
                    }
                    updateLyricsScroll(pos);
                }
            } catch (Throwable ignored) {}
            h.postDelayed(this, 500);
        }
    };

    private void startCoverRotate() {
        try {
            if (ivCover == null) return;
            ivCover.setClipToOutline(true);
            if (ivCoverWrap != null) {
                ivCoverWrap.setBackgroundResource(R.drawable.bg_cover_circle);
                ivCoverWrap.setClipToOutline(true);
                if (Build.VERSION.SDK_INT >= 21) {
                    ivCoverWrap.setOutlineProvider(new android.view.ViewOutlineProvider() {
                        @Override public void getOutline(View v, android.graphics.Outline o) {
                            o.setOval(0, 0, v.getWidth(), v.getHeight());
                        }
                    });
                }
            }
            if (coverRotate != null) coverRotate.cancel();
            coverRotate = ObjectAnimator.ofFloat(ivCover, "rotation", 0f, 360f);
            coverRotate.setDuration(20000);
            coverRotate.setRepeatCount(ObjectAnimator.INFINITE);
            coverRotate.setInterpolator(new LinearInterpolator());
            coverRotate.start();
        } catch (Throwable ignored) {}
    }

    private void stopCoverRotate() {
        try {
            if (coverRotate != null) coverRotate.cancel();
            if (ivCover != null) ivCover.setRotation(0f);
        } catch (Throwable ignored) {}
    }

    private void bounce(View v) {
        try {
            v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(80).start();
            v.postDelayed(new Runnable() {
                @Override public void run() {
                    try {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                            .setInterpolator(new OvershootInterpolator(2f)).start();
                    } catch (Throwable ignored) {}
                }
            }, 80);
        } catch (Throwable ignored) {}
    }

    private void togglePlay() {
        try {
            if (player == null) return;
            if (player.isPlaying()) player.pause(); else player.play();
            updatePlayIcon();
        } catch (Throwable ignored) {}
    }

    private void updatePlayIcon() {
        try {
            if (btnPlay == null || player == null) return;
            btnPlay.setImageResource(player.isPlaying()
                ? android.R.drawable.ic_media_pause
                : android.R.drawable.ic_media_play);
            if (player.isPlaying()) startCoverRotate(); else stopCoverRotate();
        } catch (Throwable ignored) {}
    }

    private void downloadCurrent() {
        try {
            if (currentIndex < 0 || currentIndex >= queue.size()) return;
            final Song s = queue.get(currentIndex);
            NiceToast.show(this, "开始下载：" + s.title);
            final String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_")
                + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
            if (s.isOnline) {
                NeteaseApi.getPlayUrl(s.id, new NeteaseApi.OnUrl() {
                    @Override public void onResult(String url) {
                        if (url == null || url.isEmpty()) {
                            NiceToast.show(PlayerActivity.this, "无地址");
                            return;
                        }
                        com.music.app.util.DownloadUtil.download(url, safeName + ".mp3",
                            new com.music.app.util.DownloadUtil.Callback() {
                                @Override public void onDone(boolean ok, String path) {
                                    NiceToast.show(PlayerActivity.this, ok ? "✓ 已下载" : "下载失败");
                                }
                            });
                    }
                });
            } else {
                NiceToast.show(this, "本地音乐已在设备上");
            }
        } catch (Throwable ignored) {}
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        h.removeCallbacksAndMessages(null);
        try { if (glowPulse != null) glowPulse.cancel(); } catch (Throwable ignored) {}
        try { if (coverRotate != null) coverRotate.cancel(); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
