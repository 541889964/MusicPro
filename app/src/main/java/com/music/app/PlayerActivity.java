package com.music.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.model.Song;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {
    public static ExoPlayer player;
    public static List<Song> queue = new ArrayList<>();
    public static int currentIndex = 0;
    public static String currentLyric = "";

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCur, tvTot;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose;
    private ImageView ivCover;
    private boolean drag = false;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try { setContentView(R.layout.activity_player); run(); }
        catch (Throwable t) { android.util.Log.e("Music", "Player", t); finish(); }
    }

    private void run() {
        seek = findViewById(R.id.seek);
        tvTitle = findViewById(R.id.tvTitle);
        tvArtist = findViewById(R.id.tvArtist);
        tvCur = findViewById(R.id.tvCur);
        tvTot = findViewById(R.id.tvTot);
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnClose = findViewById(R.id.btnClose);
        ivCover = findViewById(R.id.ivCover);

        int idx = getIntent().getIntExtra("index", 0);
        if (queue.isEmpty()) { finish(); return; }
        if (idx < 0 || idx >= queue.size()) idx = 0;
        currentIndex = idx;

        if (player == null) player = new ExoPlayer.Builder(this).build();
        playAt(currentIndex);

        if (ivCover != null) {
            Bitmap cover = WallpaperHelper.generateCircleCover(400);
            if (cover != null) ivCover.setImageBitmap(cover);
        }

        btnPlay.setOnClickListener(v -> { toggle(); press(v); });
        btnPrev.setOnClickListener(v -> { if (currentIndex > 0) playAt(currentIndex - 1); press(v); });
        btnNext.setOnClickListener(v -> { if (currentIndex < queue.size() - 1) playAt(currentIndex + 1); press(v); });
        btnClose.setOnClickListener(v -> finish());

        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                if (u && tvCur != null) tvCur.setText(fmt(p));
            }
            public void onStartTrackingTouch(SeekBar sb) { drag = true; }
            public void onStopTrackingTouch(SeekBar sb) {
                if (player != null) player.seekTo(sb.getProgress());
                drag = false;
            }
        });

        h.post(new Runnable() {
            @Override public void run() { updateProgress(); h.postDelayed(this, 250); }
        });
    }

    private void press(View v) {
        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(70)
            .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(200)
                .setInterpolator(new OvershootInterpolator(2f)).start()).start();
    }

    private void playAt(int i) {
        if (i < 0 || i >= queue.size()) return;
        currentIndex = i;
        Song s = queue.get(i);
        if (tvTitle != null) tvTitle.setText(s.title);
        if (tvArtist != null) tvArtist.setText(s.artist);
        if (tvTot != null) tvTot.setText(s.getDur());
        try {
            MediaItem item = MediaItem.fromUri("file://" + s.path);
            player.setMediaItem(item);
            player.prepare();
            player.play();
            updatePlayIcon();
        } catch (Throwable ignored) {}
    }

    private void toggle() {
        if (player == null) return;
        if (player.isPlaying()) player.pause(); else player.play();
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
            if (!drag) { seek.setMax((int) dur); seek.setProgress((int) pos); }
            if (tvCur != null) tvCur.setText(fmt((int) pos));
        }
    }

    private String fmt(int ms) { int s = ms / 1000; return String.format("%d:%02d", s / 60, s % 60); }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
