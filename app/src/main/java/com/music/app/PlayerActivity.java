package com.music.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
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
    private boolean needReplaceFirst = false;

    private void startIsland() {
        try {
            Intent it = new Intent(this, com.music.app.service.IslandService.class);
            startService(it);
        } catch (Throwable ignored) {}
    }

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_player);
        ivBg = findViewById(R.id.ivPlayerBg);
        Bitmap bm = WallpaperHelper.loadCurrent(this);
        if (bm != null && ivBg != null) ivBg.setImageBitmap(bm);

        tvTitle = findViewById(R.id.tvPlayerTitle);
        tvArtist = findViewById(R.id.tvPlayerArtist);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvTotal = findViewById(R.id.tvTotal);
        seek = findViewById(R.id.seekBar);
        ivCover = findViewById(R.id.ivCover);
        ivCoverWrap = findViewById(R.id.ivCoverWrap);
        glowBehindCover = findViewById(R.id.glowBehindCover);
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnClose = findViewById(R.id.btnClose);
        btnDownload = findViewById(R.id.btnDownload);
        btnFav = findViewById(R.id.btnFav);
        btnShare = findViewById(R.id.btnShare);
        lyricsScroll = findViewById(R.id.lyricsScroll);
        lyricsContainer = findViewById(R.id.lyricsContainer);
        tvLyricsEmpty = findViewById(R.id.tvLyricsEmpty);

        int startIdx = getIntent().getIntExtra("index", 0);
        if (queue == null || queue.isEmpty()) { NiceToast.show(this, "播放列表为空"); finish(); return; }
        if (startIdx < 0 || startIdx >= queue.size()) startIdx = 0;
        currentIndex = startIdx;

        initPlayer();
        // 立即播
        playAt(currentIndex);
        startIsland();

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); togglePlay(); }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); if (currentIndex > 0) playAt(currentIndex - 1); }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); if (currentIndex < queue.size() - 1) playAt(currentIndex + 1); }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        if (btnDownload != null) btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); downloadCurrent(); }
        });
        if (btnFav != null) btnFav.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); heartbeat(v); NiceToast.love(PlayerActivity.this, "已加入喜欢"); }
        });
        if (btnShare != null) btnShare.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); NiceToast.show(PlayerActivity.this, "分享开发中"); }
        });
        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    if (fromUser && tvCurrent != null) tvCurrent.setText(fmt(p));
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) {
                    if (player != null) player.seekTo(sb.getProgress());
                    dragging = false;
                }
            });
        }
        startGlowPulse();
        h.post(new Runnable() {
            @Override public void run() { updateProgress(); h.postDelayed(this, 200); }
        });
        View card = findViewById(R.id.playerCard);
        if (card != null) {
            card.setAlpha(0f); card.setTranslationY(60f);
            card.animate().alpha(1f).translationY(0f).setDuration(650)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
        if (ivCoverWrap != null) {
            ivCoverWrap.setAlpha(0f); ivCoverWrap.setScaleX(0.7f); ivCoverWrap.setScaleY(0.7f);
            ivCoverWrap.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setStartDelay(150).setDuration(700).setInterpolator(new OvershootInterpolator(1.1f)).start();
        }
    }

    private void initPlayer() {
        player = MusicService.ensurePlayer(this);
        if (!listenerBound) {
            listenerBound = true;
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED && currentIndex < queue.size() - 1) {
                        playAt(currentIndex + 1);
                    }
                }
                @Override public void onMediaItemTransition(MediaItem item, int reason) {
                    if (item != null) {
                        int idx = player.getCurrentMediaItemIndex();
                        currentIndex = idx;
                        updateTitleFromItem(item);
                        startCoverRotate();
                    }
                }
                @Override public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) startCoverRotate(); else stopCoverRotate();
                    updatePlayIcon();
                }
            });
        }
    }

    private void updateTitleFromItem(MediaItem item) {
        if (item == null) return;
        MediaMetadata md = item.mediaMetadata;
        if (md == null) return;
        CharSequence t = md.title;
        CharSequence a = md.artist;
        if (t != null && tvTitle != null) tvTitle.setText(t);
        if (a != null && tvArtist != null) tvArtist.setText(a);
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
    }

    /** ★ 秒播：立即用外链 URL 建队播放，后台异步拿真实 URL 替换 */
    private void playAt(int idx) {
        if (idx < 0 || idx >= queue.size()) return;
        currentIndex = idx;
        final Song song = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(song.title);
        if (tvArtist != null) tvArtist.setText(song.artist);
        if (tvTotal != null) tvTotal.setText(song.getDurationText());

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

        // 歌词
        if (song.lyric != null && !song.lyric.isEmpty()) buildLyricsView(song.lyric);
        else if (song.isOnline) loadLyrics(song.id, idx);
        else loadLocalLyrics(song);

        // ★ 立即构建整队并播放（用外链 URL，ExoPlayer 会跟随 302 重定向）
        List<MediaItem> items = new ArrayList<MediaItem>();
        for (int i = 0; i < queue.size(); i++) {
            Song sg = queue.get(i);
            String uri;
            if (sg.isOnline) {
                if (sg.onlineUrl == null || sg.onlineUrl.isEmpty()) {
                    // 外链 URL（ExoPlayer 自动重定向）
                    sg.onlineUrl = "https://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3";
                }
                uri = sg.onlineUrl;
            } else {
                uri = "file://" + sg.path;
            }
            items.add(new MediaItem.Builder().setUri(uri)
                .setMediaMetadata(new MediaMetadata.Builder()
                    .setTitle(sg.title).setArtist(sg.artist).build())
                .build());
        }
        MusicService.playItems(items, idx);
        updatePlayIcon();

        // 后台预缓存真实 URL（仅在线歌曲）
        preCacheRealUrls(idx, 30);
    }

    /** 后台线程：顺序请求队列中在线歌曲的真实 320k URL，最多缓存 30 首 */
    private void preCacheRealUrls(final int startIdx, final int maxCount) {
        new Thread(new Runnable() {
            @Override public void run() {
                try { Thread.sleep(2000); } catch (Throwable ignored) {}
                int count = 0;
                for (int i = 0; i < queue.size() && count < maxCount; i++) {
                    if (i == startIdx) continue;
                    final Song sg = queue.get(i);
                    if (!sg.isOnline) continue;
                    // 已经是真实 URL（含 stream/ 路径）就跳过
                    if (sg.onlineUrl != null && sg.onlineUrl.contains("/stream/")) {
                        count++;
                        continue;
                    }
                    final java.util.concurrent.CountDownLatch latch =
                        new java.util.concurrent.CountDownLatch(1);
                    NeteaseApi.getPlayUrl(sg.id, new NeteaseApi.OnUrl() {
                        @Override public void onResult(String url) {
                            if (url != null && !url.isEmpty()) {
                                sg.onlineUrl = url;
                                // 更新正在播放的 mediaItem（如果轮到它）
                                try {
                                    androidx.media3.exoplayer.ExoPlayer p = MusicService.getPlayer();
                                    if (p != null) {
                                        int cur = p.getCurrentMediaItemIndex();
                                        if (cur == queue.indexOf(sg)) {
                                            // 当前正在播这首歌就跳过，别打断
                                        }
                                    }
                                } catch (Throwable ignored) {}
                            }
                            latch.countDown();
                        }
                    });
                    try { latch.await(4, java.util.concurrent.TimeUnit.SECONDS); }
                    catch (Throwable ignored) {}
                    count++;
                    try { Thread.sleep(150); } catch (Throwable ignored) {}
                }
            }
        }).start();
    }

    private void loadLyrics(final long songId, final int idx) {
        if (lyricsContainer != null) lyricsContainer.removeAllViews();
        if (tvLyricsEmpty != null) { tvLyricsEmpty.setVisibility(View.VISIBLE); tvLyricsEmpty.setText("♪ 歌词加载中…"); }
        lyrics = new ArrayList<LyricsParser.Line>();
        lyricViews = new ArrayList<TextView>();
        currentLine = -1;
        NeteaseApi.getLyrics(songId, new NeteaseApi.OnLyrics() {
            @Override public void onResult(String lrc) {
                if (lrc == null || lrc.isEmpty()) {
                    if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
                    return;
                }
                if (idx >= 0 && idx < queue.size()) queue.get(idx).lyric = lrc;
                buildLyricsView(lrc);
            }
        });
    }

    private void loadLocalLyrics(Song song) {
        if (lyricsContainer != null) lyricsContainer.removeAllViews();
        lyrics = new ArrayList<LyricsParser.Line>();
        lyricViews = new ArrayList<TextView>();
        currentLine = -1;
        try {
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
        } catch (Throwable ignored) {}
        if (tvLyricsEmpty != null) { tvLyricsEmpty.setVisibility(View.VISIBLE); tvLyricsEmpty.setText("♪ 暂无歌词"); }
    }

    private void buildLyricsView(String lrc) {
        lyrics = LyricsParser.parse(lrc);
        if (lyrics.isEmpty()) { if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词"); return; }
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
        top.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(180 * d)));
        View bottom = new View(this);
        bottom.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(180 * d)));
        lyricsContainer.addView(top, 0);
        lyricsContainer.addView(bottom);
    }

    private void updateLyricsScroll(long pos) {
        if (lyrics.isEmpty() || lyricViews.isEmpty()) return;
        int idx = LyricsParser.findIndex(lyrics, pos);
        if (idx == currentLine) return;
        if (idx < 0 || idx >= lyricViews.size()) return;
        int old = currentLine;
        currentLine = idx;
        TextView cur = lyricViews.get(currentLine);
        cur.animate().alpha(1f).scaleX(1.06f).scaleY(1.06f).setDuration(350).setInterpolator(new DecelerateInterpolator()).start();
        cur.setTextColor(0xFFFF6B9D);
        cur.setTextSize(17);
        if (old >= 0 && old < lyricViews.size()) {
            TextView o = lyricViews.get(old);
            o.animate().alpha(0.5f).scaleX(1f).scaleY(1f).setDuration(350).setInterpolator(new DecelerateInterpolator()).start();
            o.setTextColor(0x66FFFFFF); o.setTextSize(15);
        }
        for (int i = 0; i < lyricViews.size(); i++) {
            int dd = Math.abs(i - currentLine);
            TextView tv = lyricViews.get(i);
            if (i == currentLine) continue;
            if (dd == 1) { tv.setTextColor(0x99FFFFFF); tv.setAlpha(0.85f); }
            else if (dd == 2) { tv.setTextColor(0x77FFFFFF); tv.setAlpha(0.7f); }
            else if (dd <= 4) { tv.setTextColor(0x66FFFFFF); tv.setAlpha(0.55f); }
            else { tv.setTextColor(0x44FFFFFF); tv.setAlpha(0.4f); }
        }
        if (lyricsScroll != null) {
            final View v = lyricViews.get(currentLine);
            lyricsScroll.post(new Runnable() {
                @Override public void run() {
                    int target = v.getTop() - lyricsScroll.getHeight() / 2 + v.getHeight() / 2;
                    lyricsScroll.smoothScrollTo(0, Math.max(0, target));
                }
            });
        }
    }

    private void startGlowPulse() {
        if (glowBehindCover == null) return;
        glowPulse = ObjectAnimator.ofFloat(glowBehindCover, "alpha", 0.35f, 0.85f);
        glowPulse.setDuration(1800);
        glowPulse.setRepeatCount(ObjectAnimator.INFINITE);
        glowPulse.setRepeatMode(ObjectAnimator.REVERSE);
        glowPulse.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulse.start();
    }

    private void startCoverRotate() {
        if (ivCover == null) return;
        if (coverRotate != null) coverRotate.cancel();
        coverRotate = ObjectAnimator.ofFloat(ivCover, "rotation", 0f, 360f);
        coverRotate.setDuration(20000);
        coverRotate.setRepeatCount(ObjectAnimator.INFINITE);
        coverRotate.setInterpolator(new LinearInterpolator());
        coverRotate.start();
    }

    private void stopCoverRotate() {
        if (coverRotate != null) coverRotate.cancel();
        if (ivCover != null) ivCover.setRotation(0f);
    }

    private void bounce(View v) {
        v.animate().scaleX(0.86f).scaleY(0.86f).setDuration(80).start();
        v.postDelayed(new Runnable() {
            @Override public void run() {
                v.animate().scaleX(1f).scaleY(1f).setDuration(220)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
            }
        }, 80);
    }

    private void heartbeat(View v) {
        ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.25f, 1f, 1.15f, 1f).setDuration(700).start();
        ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.25f, 1f, 1.15f, 1f).setDuration(700).start();
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
        updatePlayIcon();
    }

    private void updatePlayIcon() {
        if (btnPlay == null || player == null) return;
        btnPlay.setImageResource(player.isPlaying() ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
        if (player.isPlaying()) startCoverRotate(); else stopCoverRotate();
    }

    private void updateProgress() {
        if (player == null || seek == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0) {
            if (!dragging) { seek.setMax((int) dur); seek.setProgress((int) pos); }
            if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
        }
        updateLyricsScroll(pos);
    }

    private void downloadCurrent() {
        if (currentIndex < 0 || currentIndex >= queue.size()) return;
        final Song s = queue.get(currentIndex);
        NiceToast.show(this, "开始下载：" + s.title);
        final String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_") + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");
        if (s.isOnline) {
            NeteaseApi.getPlayUrl(s.id, new NeteaseApi.OnUrl() {
                @Override public void onResult(String url) {
                    if (url == null || url.isEmpty()) { NiceToast.show(PlayerActivity.this, "无地址"); return; }
                    com.music.app.util.DownloadUtil.download(url, safeName + ".mp3",
                        new com.music.app.util.DownloadUtil.Callback() {
                            @Override public void onDone(boolean ok, String path) {
                                NiceToast.show(PlayerActivity.this, ok ? "✓ 已下载" : "下载失败");
                            }
                        });
                }
            });
        } else NiceToast.show(this, "本地音乐已在设备上");
    }

    private String fmt(int ms) { int s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60); }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (glowPulse != null) glowPulse.cancel();
        if (coverRotate != null) coverRotate.cancel();
        super.onDestroy();
    }
}
