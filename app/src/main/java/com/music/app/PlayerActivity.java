package com.music.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.model.Song;
import com.music.app.service.MusicService;
import com.music.app.util.LyricsParser;
import com.music.app.util.NiceToast;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    public static List<Song> queue = new ArrayList<Song>();
    public static int currentIndex = 0;
    public static String currentLyric = "";

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageView ivCover, ivBg;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose;
    private android.widget.ScrollView lyricsScroll;
    private LinearLayout lyricsContainer;
    private TextView tvLyricsEmpty;
    private List<LyricsParser.Line> lyrics = new ArrayList<LyricsParser.Line>();
    private List<TextView> lyricViews = new ArrayList<TextView>();
    private int currentLine = -1;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_player);
            ivBg = findViewById(R.id.ivPlayerBg);
            Bitmap bg = WallpaperHelper.loadCurrent(this);
            if (bg != null) ivBg.setImageBitmap(bg);

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
            if (currentIndex < 0 || currentIndex >= queue.size()) currentIndex = 0;

            MusicService.ensurePlayer(this);
            playAt(currentIndex);

            if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { MusicService.toggle(PlayerActivity.this); }
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
            if (seek != null) {
                seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                        if (u && tvCurrent != null) tvCurrent.setText(fmt(p));
                    }
                    @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                    @Override public void onStopTrackingTouch(SeekBar sb) {
                        MusicService.seekToPos(sb.getProgress());
                        dragging = false;
                    }
                });
            }
            h.post(tick);
        } catch (Throwable t) { finish(); }
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            try {
                if (MusicService.sharedPlayer != null && seek != null) {
                    long pos = MusicService.getPos();
                    long dur = MusicService.getDur();
                    if (dur > 0) {
                        if (!dragging) { seek.setMax((int)dur); seek.setProgress((int)pos); }
                        if (tvCurrent != null) tvCurrent.setText(fmt((int)pos));
                        if (tvTotal != null) tvTotal.setText(fmt((int)dur));
                    }
                    updateLyrics(pos);
                    if (btnPlay != null)
                        btnPlay.setImageResource(MusicService.isPlaying()
                            ? android.R.drawable.ic_media_pause
                            : android.R.drawable.ic_media_play);
                }
            } catch (Throwable ignored) {}
            h.postDelayed(this, 300);
        }
    };

    private void playAt(int idx) {
        if (idx < 0 || idx >= queue.size()) return;
        currentIndex = idx;
        Song song = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(song.title);
        if (tvArtist != null) tvArtist.setText(song.artist);
        if (tvTotal != null) tvTotal.setText(song.getDurationText());
        if (ivCover != null) {
            String name = WallpaperHelper.forSong(this, song.id);
            Bitmap bm = WallpaperHelper.load(this, name);
            if (bm != null) ivCover.setImageBitmap(bm);
        }
        List<MediaItem> items = new ArrayList<MediaItem>();
        for (Song sg : queue) {
            String uri = sg.isOnline
                ? (sg.onlineUrl != null && !sg.onlineUrl.isEmpty() ? sg.onlineUrl
                    : "http://music.163.com/song/media/outer/url?id=" + sg.id + ".mp3")
                : "file://" + sg.path;
            items.add(new MediaItem.Builder().setUri(uri)
                .setMediaMetadata(new MediaMetadata.Builder()
                    .setTitle(sg.title).setArtist(sg.artist).build()).build());
        }
        MusicService.playItems(items, idx);
        loadLyrics(song);
    }

    private void loadLyrics(Song song) {
        try {
            currentLyric = "";
            if (lyricsContainer != null) lyricsContainer.removeAllViews();
            lyrics = new ArrayList<LyricsParser.Line>();
            lyricViews = new ArrayList<TextView>();
            currentLine = -1;
            if (tvLyricsEmpty != null) tvLyricsEmpty.setText("♪ 暂无歌词");
            // 尝试同目录 .lrc
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
                buildLyrics(song.lyric);
            }
        } catch (Throwable ignored) {}
    }

    private void buildLyrics(String lrc) {
        try {
            lyrics = LyricsParser.parse(lrc);
            currentLyric = lrc;
            if (lyrics.isEmpty()) return;
            if (tvLyricsEmpty != null) tvLyricsEmpty.setVisibility(View.GONE);
            lyricsContainer.removeAllViews();
            lyricViews.clear();
            float d = getResources().getDisplayMetrics().density;
            for (int i = 0; i < lyrics.size(); i++) {
                TextView tv = new TextView(this);
                tv.setText(lyrics.get(i).text);
                tv.setTextColor(0x66FFFFFF);
                tv.setTextSize(15);
                tv.setPadding((int)(24*d),(int)(10*d),(int)(24*d),(int)(10*d));
                tv.setGravity(android.view.Gravity.CENTER);
                lyricsContainer.addView(tv);
                lyricViews.add(tv);
            }
            View top = new View(this);
            top.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(180*d)));
            View bottom = new View(this);
            bottom.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(180*d)));
            lyricsContainer.addView(top, 0);
            lyricsContainer.addView(bottom);
        } catch (Throwable ignored) {}
    }

    private void updateLyrics(long pos) {
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
                o.setTextColor(0x66FFFFFF);
                o.setTextSize(15);
            }
            if (lyricsScroll != null) {
                final View v = lyricViews.get(currentLine);
                lyricsScroll.post(new Runnable() {
                    @Override public void run() {
                        try {
                            int target = v.getTop() - lyricsScroll.getHeight()/2 + v.getHeight()/2;
                            lyricsScroll.smoothScrollTo(0, Math.max(0, target));
                        } catch (Throwable ignored) {}
                    }
                });
            }
        } catch (Throwable ignored) {}
    }

    private String fmt(int ms) {
        if (ms < 0) ms = 0;
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
