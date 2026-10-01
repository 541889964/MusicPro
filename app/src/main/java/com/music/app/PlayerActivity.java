package com.music.app;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import com.music.app.model.Song;
import com.music.app.service.MusicService;
import com.music.app.util.LyricsParser;
import com.music.app.util.NeteaseApi;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    public static List<Song> queue = new ArrayList<Song>();
    public static int currentIndex = 0;
    public static String currentLyric = "";
    private static boolean bound = false;

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageView ivCover;
    private TextView btnPlay, btnPrev, btnNext, btnClose;
    private android.widget.ScrollView lyricsScroll;
    private LinearLayout lyricsContainer;
    private TextView tvLyricsEmpty;
    private List<LyricsParser.Line> lyrics = new ArrayList<LyricsParser.Line>();
    private List<TextView> lyricViews = new ArrayList<TextView>();
    private int currentLine = -1;
    private ObjectAnimator coverRotate;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging = false, destroyed = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try { setContentView(R.layout.activity_player); }
        catch (Throwable t) { finish(); return; }

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
        lyricsScroll = findViewById(R.id.lyricsScroll);
        lyricsContainer = findViewById(R.id.lyricsContainer);
        tvLyricsEmpty = findViewById(R.id.tvLyricsEmpty);

        if (queue == null || queue.isEmpty()) { finish(); return; }
        int idx = getIntent().getIntExtra("index", 0);
        if (idx < 0 || idx >= queue.size()) idx = 0;
        currentIndex = idx;

        MusicService.ensurePlayer(this);
        bindListener();
        playAt(currentIndex);

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (MusicService.isPlaying()) MusicService.pause(PlayerActivity.this);
                else MusicService.play(PlayerActivity.this);
                updatePlayIcon();
            }
        });
        if (btnPrev != null) btnPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MusicService.prev(PlayerActivity.this); }
        });
        if (btnNext != null) btnNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { MusicService.next(PlayerActivity.this); }
        });
        if (btnClose != null) btnClose.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        if (seek != null) seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (u && tvCurrent != null) tvCurrent.setText(fmt(p));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                MusicService.seekToPos(sb.getProgress());
                dragging = false;
            }
        });

        try {
            if (Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this)) {
                Intent isl = new Intent(this, com.music.app.service.IslandService.class);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(isl);
                else startService(isl);
            }
        } catch (Throwable ignored) {}

        h.post(progressTick);
    }

    private void bindListener() {
        if (bound || MusicService.getPlayer() == null) return;
        bound = true;
        MusicService.getPlayer().addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item, int r) {
                if (destroyed || item == null) return;
                currentIndex = MusicService.getIndex();
                updateTitle(item);
                startRotate();
            }
            @Override public void onIsPlayingChanged(boolean p) {
                if (destroyed) return;
                if (p) startRotate(); else stopRotate();
                updatePlayIcon();
            }
        });
    }

    private void updateTitle(MediaItem item) {
        if (item == null || item.mediaMetadata == null) return;
        MediaMetadata md = item.mediaMetadata;
        if (md.title != null && tvTitle != null) tvTitle.setText(md.title);
        if (md.artist != null && tvArtist != null) tvArtist.setText(md.artist);
        int idx = MusicService.getIndex();
        if (idx >= 0 && idx < queue.size()) {
            Song s = queue.get(idx);
            Bitmap bm = WallpaperHelper.load(this, WallpaperHelper.forSong(this, s.id));
            if (bm != null && ivCover != null) ivCover.setImageBitmap(bm);
            if (s.lyric != null && !s.lyric.isEmpty()) buildLyrics(s.lyric);
            else if (s.isOnline) loadLyrics(s.id, idx);
            else loadLocalLyrics(s);
        }
    }

    private void playAt(int idx) {
        if (idx < 0 || idx >= queue.size()) return;
        currentIndex = idx;
        Song s = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(s.title);
        if (tvArtist != null) tvArtist.setText(s.artist);
        if (tvTotal != null) tvTotal.setText(s.getDurationText());
        if (ivCover != null) {
            Bitmap bm = WallpaperHelper.load(this, WallpaperHelper.forSong(this, s.id));
            if (bm != null) ivCover.setImageBitmap(bm);
        }
        currentLyric = "";
        if (s.lyric != null && !s.lyric.isEmpty()) buildLyrics(s.lyric);
        else if (s.isOnline) loadLyrics(s.id, idx);
        else loadLocalLyrics(s);

        if (s.isOnline && (s.onlineUrl == null || s.onlineUrl.isEmpty())) {
            NeteaseApi.getPlayUrl(s.id, new NeteaseApi.OnUrl() {
                @Override public void onResult(String url) {
                    s.onlineUrl = url;
                    MusicService.setQueue(PlayerActivity.this, queue, currentIndex);
                    updatePlayIcon();
                }
            });
        } else {
            MusicService.setQueue(this, queue, idx);
            updatePlayIcon();
        }
    }

    private void loadLyrics(final long id, final int idx) {
        if (lyricsContainer != null) lyricsContainer.removeAllViews();
        if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.VISIBLE);
        lyrics = new ArrayList<LyricsParser.Line>();
        lyricViews = new ArrayList<TextView>();
        currentLine = -1;
        NeteaseApi.getLyrics(id, new NeteaseApi.OnLyrics() {
            @Override public void onResult(String lrc) {
                if (lrc == null || lrc.isEmpty()) return;
                if (idx >= 0 && idx < queue.size()) queue.get(idx).lyric = lrc;
                buildLyrics(lrc);
            }
        });
    }

    private void loadLocalLyrics(Song song) {
        try {
            String p = song.path;
            int dot = p.lastIndexOf('.');
            if (dot > 0) p = p.substring(0, dot) + ".lrc";
            java.io.File f = new java.io.File(p);
            if (f.exists()) {
                java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(f), "UTF-8"));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append("\n");
                r.close();
                song.lyric = sb.toString();
                buildLyrics(song.lyric);
                return;
            }
        } catch (Throwable ignored) {}
        if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.VISIBLE);
    }

    private void buildLyrics(String lrc) {
        currentLyric = lrc;
        lyrics = LyricsParser.parse(lrc);
        if (lyrics.isEmpty()) { if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.VISIBLE); return; }
        if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.GONE);
        if (lyricsContainer == null) return;
        lyricsContainer.removeAllViews();
        lyricViews.clear();
        float d = getResources().getDisplayMetrics().density;
        for (int i = 0; i < lyrics.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(lyrics.get(i).text);
            tv.setTextColor(0x66FFFFFF);
            tv.setTextSize(15);
            tv.setPadding((int)(20*d), (int)(8*d), (int)(20*d), (int)(8*d));
            tv.setGravity(android.view.Gravity.CENTER);
            lyricsContainer.addView(tv);
            lyricViews.add(tv);
        }
        View top = new View(this);
        top.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(180*d)));
        View bot = new View(this);
        bot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (int)(180*d)));
        lyricsContainer.addView(top, 0);
        lyricsContainer.addView(bot);
    }

    private void updateLyricScroll(long pos) {
        if (lyrics.isEmpty() || lyricViews.isEmpty()) return;
        int idx = LyricsParser.findIndex(lyrics, pos);
        if (idx == currentLine || idx < 0 || idx >= lyricViews.size()) return;
        int old = currentLine;
        currentLine = idx;
        lyricViews.get(currentLine).setTextColor(0xFFFF6B9D);
        lyricViews.get(currentLine).setTextSize(17);
        if (old >= 0 && old < lyricViews.size()) {
            lyricViews.get(old).setTextColor(0x66FFFFFF);
            lyricViews.get(old).setTextSize(15);
        }
        if (lyricsScroll != null) {
            final View v = lyricViews.get(currentLine);
            lyricsScroll.post(new Runnable() {
                @Override public void run() {
                    int t = v.getTop() - lyricsScroll.getHeight()/2 + v.getHeight()/2;
                    lyricsScroll.smoothScrollTo(0, Math.max(0, t));
                }
            });
        }
    }

    private void startRotate() {
        if (ivCover == null) return;
        if (coverRotate != null) coverRotate.cancel();
        coverRotate = ObjectAnimator.ofFloat(ivCover, "rotation", 0f, 360f);
        coverRotate.setDuration(20000);
        coverRotate.setRepeatCount(ObjectAnimator.INFINITE);
        coverRotate.setInterpolator(new LinearInterpolator());
        coverRotate.start();
    }
    private void stopRotate() { if (coverRotate != null) coverRotate.cancel(); if (ivCover != null) ivCover.setRotation(0f); }

    private final Runnable progressTick = new Runnable() {
        @Override public void run() {
            if (destroyed) return;
            try {
                if (MusicService.getPlayer() != null && seek != null) {
                    long pos = MusicService.getPos(), dur = MusicService.getDur();
                    if (dur > 0 && !dragging) {
                        seek.setMax((int) dur);
                        seek.setProgress((int) pos);
                        if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
                    }
                    updateLyricScroll(pos);
                }
            } catch (Throwable ignored) {}
            h.postDelayed(this, 500);
        }
    };

    private void updatePlayIcon() {
        if (btnPlay == null) return;
        btnPlay.setText(MusicService.isPlaying() ? "⏸" : "▶");
        if (MusicService.isPlaying()) startRotate(); else stopRotate();
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s/60, s%60);
    }

    @Override protected void onDestroy() {
        destroyed = true;
        h.removeCallbacksAndMessages(null);
        try { if (coverRotate != null) coverRotate.cancel(); } catch (Throwable ignored) {}
        super.onDestroy();
    }
}
