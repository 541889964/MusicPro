package com.music.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.bumptech.glide.Glide;
import com.music.app.model.Song;
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

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageView ivCover;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose, btnDownload;
    private android.widget.ScrollView lyricsScroll;
    private android.widget.LinearLayout lyricsContainer;
    private TextView tvLyricsEmpty;

    private List<LyricsParser.Line> lyrics = new ArrayList<LyricsParser.Line>();
    private List<TextView> lyricViews = new ArrayList<TextView>();
    private int currentLine = -1;

    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_player);

        ImageView bg = findViewById(R.id.ivPlayerBg);
        Bitmap bm = WallpaperHelper.loadUser(this);
        if (bm != null && bg != null) bg.setImageBitmap(bm);

        tvTitle = findViewById(R.id.tvPlayerTitle);
        tvArtist = findViewById(R.id.tvPlayerArtist);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvTotal = findViewById(R.id.tvTotal);
        seek = findViewById(R.id.seekBar);
        ivCover = findViewById(R.id.ivCover);
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnClose = findViewById(R.id.btnClose);
        btnDownload = findViewById(R.id.btnDownload);
        lyricsScroll = findViewById(R.id.lyricsScroll);
        lyricsContainer = findViewById(R.id.lyricsContainer);
        tvLyricsEmpty = findViewById(R.id.tvLyricsEmpty);

        int startIdx = getIntent().getIntExtra("index", 0);
        if (queue == null || queue.isEmpty()) {
            NiceToast.show(this, "播放列表为空");
            finish();
            return;
        }
        if (startIdx < 0 || startIdx >= queue.size()) startIdx = 0;
        currentIndex = startIdx;

        initPlayer();
        playAt(currentIndex);

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { togglePlay(); }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentIndex > 0) playAt(currentIndex - 1);
            }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (currentIndex < queue.size() - 1) playAt(currentIndex + 1);
            }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        if (btnDownload != null) btnDownload.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                downloadCurrent();
            }
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

        h.post(new Runnable() {
            @Override public void run() {
                updateProgress();
                h.postDelayed(this, 250);
            }
        });

        View card = findViewById(R.id.playerCard);
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(80f);
            card.animate().alpha(1f).translationY(0f)
                .setDuration(600)
                .setInterpolator(new DecelerateInterpolator()).start();
        }
    }

    private void initPlayer() {
        if (player == null) {
            player = new ExoPlayer.Builder(this).build();
            player.addListener(new Player.Listener() {
                @Override public void onPlaybackStateChanged(int state) {
                    if (state == Player.STATE_ENDED) {
                        if (currentIndex < queue.size() - 1) playAt(currentIndex + 1);
                    }
                }
            });
        }
    }

    private void playAt(int idx) {
        if (idx < 0 || idx >= queue.size() || player == null) return;
        currentIndex = idx;
        final Song song = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(song.title);
        if (tvArtist != null) tvArtist.setText(song.artist);
        if (tvTotal != null) tvTotal.setText(song.getDurationText());

        if (ivCover != null) {
            if (song.cover != null && !song.cover.isEmpty()) {
                Glide.with(this).load(song.cover).into(ivCover);
            } else {
                ivCover.setImageResource(R.mipmap.ic_launcher);
            }
        }

        // 获取播放地址
        if (song.isOnline) {
            // 在线：先拿 URL
            NiceToast.show(this, "正在加载…");
            NeteaseApi.getPlayUrl(song.id, new NeteaseApi.OnUrl() {
                @Override public void onResult(String url) {
                    if (url == null || url.isEmpty()) {
                        NiceToast.show(PlayerActivity.this, "无法播放，可能需VIP");
                        return;
                    }
                    song.onlineUrl = url;
                    try {
                        MediaItem item = MediaItem.fromUri(url);
                        player.setMediaItem(item);
                        player.prepare();
                        player.play();
                        updatePlayIcon();
                    } catch (Throwable t) {
                        NiceToast.show(PlayerActivity.this, "播放失败");
                    }
                }
            });
            // 加载歌词
            loadLyrics(song.id);
        } else {
            // 本地：直接播 + 尝试加载同名 .lrc
            try {
                MediaItem item = MediaItem.fromUri("file://" + song.path);
                player.setMediaItem(item);
                player.prepare();
                player.play();
                updatePlayIcon();
            } catch (Throwable t) {
                NiceToast.show(this, "播放失败");
            }
            loadLocalLyrics(song);
        }
    }

    private void loadLyrics(final long songId) {
        if (lyricsContainer != null) lyricsContainer.removeAllViews();
        if (tvLyricsEmpty != null) tvLyricsEmpty.setText("正在加载歌词…");
        lyrics = new ArrayList<LyricsParser.Line>();
        lyricViews = new ArrayList<TextView>();
        currentLine = -1;
        NeteaseApi.getLyrics(songId, new NeteaseApi.OnLyrics() {
            @Override public void onResult(String lrc) {
                if (lrc == null || lrc.isEmpty()) {
                    if (tvLyricsEmpty != null) tvLyricsEmpty.setText("暂无歌词 ♡");
                    return;
                }
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
                buildLyricsView(sb.toString());
                return;
            }
        } catch (Throwable ignored) {}
        if (tvLyricsEmpty != null) {
            tvLyricsEmpty.setVisibility(View.VISIBLE);
            tvLyricsEmpty.setText("暂无歌词 ♡");
        }
    }

    private void buildLyricsView(String lrc) {
        lyrics = LyricsParser.parse(lrc);
        if (lyrics.isEmpty()) {
            if (tvLyricsEmpty != null) {
                tvLyricsEmpty.setVisibility(View.VISIBLE);
                tvLyricsEmpty.setText("暂无歌词 ♡");
            }
            return;
        }
        if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.GONE);
        if (lyricsContainer != null) {
            lyricsContainer.removeAllViews();
            lyricViews.clear();
            float density = getResources().getDisplayMetrics().density;
            int paddingH = (int)(32 * density);
            int paddingV = (int)(10 * density);
            for (int i = 0; i < lyrics.size(); i++) {
                TextView tv = new TextView(this);
                tv.setText(lyrics.get(i).text);
                tv.setTextColor(0x99FFFFFF);
                tv.setTextSize(15);
                tv.setPadding(paddingH, paddingV, paddingH, paddingV);
                tv.setGravity(android.view.Gravity.CENTER);
                lyricsContainer.addView(tv);
                lyricViews.add(tv);
            }
            // 上下 padding 让第一句和最后一句居中
            View top = new View(this);
            top.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (int)(160 * density)));
            View bottom = new View(this);
            bottom.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT, (int)(160 * density)));
            lyricsContainer.addView(top, 0);
            lyricsContainer.addView(bottom);
        }
    }

    private void updateLyricsScroll(long pos) {
        if (lyrics.isEmpty() || lyricViews.isEmpty()) return;
        int idx = LyricsParser.findIndex(lyrics, pos);
        if (idx == currentLine) return;
        if (idx < 0 || idx >= lyricViews.size()) return;
        int old = currentLine;
        currentLine = idx;
        for (int i = 0; i < lyricViews.size(); i++) {
            TextView tv = lyricViews.get(i);
            if (i == currentLine) {
                tv.setTextColor(0xFFFF6B9D);
                tv.setTextSize(17);
                tv.animate().alpha(1f).scaleX(1.08f).scaleY(1.08f).setDuration(280).start();
            } else if (i == old) {
                tv.setTextColor(0x99FFFFFF);
                tv.setTextSize(15);
                tv.animate().alpha(0.7f).scaleX(1f).scaleY(1f).setDuration(280).start();
            }
        }
        if (lyricsScroll != null && lyricViews.get(currentLine) != null) {
            final View v = lyricViews.get(currentLine);
            lyricsScroll.post(new Runnable() {
                @Override public void run() {
                    int target = v.getTop() - lyricsScroll.getHeight() / 2 + v.getHeight() / 2;
                    lyricsScroll.smoothScrollTo(0, Math.max(0, target));
                }
            });
        }
    }

    private void togglePlay() {
        if (player == null) return;
        if (player.isPlaying()) player.pause();
        else player.play();
        updatePlayIcon();
    }

    private void updatePlayIcon() {
        if (btnPlay == null || player == null) return;
        btnPlay.setImageResource(player.isPlaying()
            ? android.R.drawable.ic_media_pause
            : android.R.drawable.ic_media_play);
    }

    private void updateProgress() {
        if (player == null || seek == null) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0) {
            if (!dragging) {
                seek.setMax((int) dur);
                seek.setProgress((int) pos);
            }
            if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
        }
        updateLyricsScroll(pos);
    }

    private void downloadCurrent() {
        if (currentIndex < 0 || currentIndex >= queue.size()) return;
        final Song s = queue.get(currentIndex);
        NiceToast.show(this, "开始下载：" + s.title);
        String safeName = s.title.replaceAll("[\\\\/:*?\"<>|]", "_")
            + " - " + s.artist.replaceAll("[\\\\/:*?\"<>|]", "_");

        if (s.isOnline) {
            NeteaseApi.getPlayUrl(s.id, new NeteaseApi.OnUrl() {
                @Override public void onResult(String url) {
                    if (url == null || url.isEmpty()) {
                        NiceToast.show(PlayerActivity.this, "无法获取下载地址");
                        return;
                    }
                    com.music.app.util.DownloadUtil.download(url,
                        safeName + ".mp3",
                        new com.music.app.util.DownloadUtil.Callback() {
                            @Override public void onDone(boolean ok, String path) {
                                NiceToast.show(PlayerActivity.this,
                                    ok ? "✓ 已下载到 /sogou/" : "下载失败");
                            }
                        });
                }
            });
            // 顺便下载歌词
            NeteaseApi.getLyrics(s.id, new NeteaseApi.OnLyrics() {
                @Override public void onResult(String lrc) {
                    if (lrc != null && !lrc.isEmpty()) {
                        com.music.app.util.DownloadUtil.saveLyrics(lrc,
                            safeName + ".lrc",
                            new com.music.app.util.DownloadUtil.Callback() {
                                @Override public void onDone(boolean ok, String path) {}
                            });
                    }
                }
            });
        } else {
            NiceToast.show(this, "本地音乐已在设备上");
        }
    }

    private String fmt(int ms) {
        int sec = ms / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }

    @Override
    protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
