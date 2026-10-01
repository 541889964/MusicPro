package com.music.app;

import android.animation.ObjectAnimator;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.model.Song;
import com.music.app.service.MusicService;
import com.music.app.util.WallpaperHelper;
import java.util.ArrayList;
import java.util.List;

public class LockScreenActivity extends AppCompatActivity {
    public static List<Song> queue = new ArrayList<Song>();
    public static int currentIndex = 0;

    private ExoPlayer player;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal, tvLyric;
    private ImageView ivCover, ivBg;
    private SeekBar seek;
    private ImageButton btnPlay, btnPrev, btnNext;
    private ObjectAnimator coverRotate;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean dragging = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_lock);

        ivBg = findViewById(R.id.ivLockBg);
        ivCover = findViewById(R.id.ivLockCover);
        tvTitle = findViewById(R.id.tvLockTitle);
        tvArtist = findViewById(R.id.tvLockArtist);
        tvLyric = findViewById(R.id.tvLockLyric);
        tvCurrent = findViewById(R.id.tvLockCurrent);
        tvTotal = findViewById(R.id.tvLockTotal);
        seek = findViewById(R.id.seekLock);
        btnPlay = findViewById(R.id.btnLockPlay);
        btnPrev = findViewById(R.id.btnLockPrev);
        btnNext = findViewById(R.id.btnLockNext);

        Bitmap bm = WallpaperHelper.loadCurrent(this);
        if (bm != null && ivBg != null) ivBg.setImageBitmap(bm);

        initPlayer();
        if (queue == null || queue.isEmpty()) { finish(); return; }
        playAt(currentIndex);

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { bounce(v); toggle(); }
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
        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                    if (fu && tvCurrent != null) tvCurrent.setText(fmt(p));
                }
                @Override public void onStartTrackingTouch(SeekBar sb) { dragging = true; }
                @Override public void onStopTrackingTouch(SeekBar sb) {
                    if (player != null) player.seekTo(sb.getProgress());
                    dragging = false;
                }
            });
        }

        h.post(new Runnable() {
            @Override public void run() { tick(); h.postDelayed(this, 300); }
        });

        View card = findViewById(R.id.lockCard);
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(80f);
            card.animate().alpha(1f).translationY(0f)
                .setDuration(500).setInterpolator(new OvershootInterpolator(1.1f)).start();
        }
    }

    private void initPlayer() {
        if (player == null) {
            if (MusicService.sharedPlayer != null) player = MusicService.sharedPlayer;
            else {
                player = new ExoPlayer.Builder(this).build();
                player.addListener(new Player.Listener() {
                    @Override public void onPlaybackStateChanged(int st) {
                        if (st == Player.STATE_ENDED && currentIndex < queue.size() - 1)
                            playAt(currentIndex + 1);
                    }
                });
            }
        }
    }

    private void playAt(int idx) {
        if (idx < 0 || idx >= queue.size() || player == null) return;
        currentIndex = idx;
        Song song = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(song.title);
        if (tvArtist != null) tvArtist.setText(song.artist);
        if (tvTotal != null) tvTotal.setText(song.getDurationText());

        String name = WallpaperHelper.forSong(this, song.id);
        Bitmap bm = WallpaperHelper.load(this, name);
        if (bm != null && ivCover != null) ivCover.setImageBitmap(bm);

        try {
            MediaItem item = MediaItem.fromUri(song.isOnline && song.onlineUrl != null
                ? song.onlineUrl : "file://" + song.path);
            player.setMediaItem(item);
            player.prepare();
            player.play();
            startCoverRotate();
        } catch (Throwable ignored) {}
        updatePlayIcon();
    }

    private void toggle() {
        if (player == null) return;
        if (player.isPlaying()) { player.pause(); stopCoverRotate(); }
        else { player.play(); startCoverRotate(); }
        updatePlayIcon();
    }

    private void updatePlayIcon() {
        if (btnPlay == null || player == null) return;
        btnPlay.setImageResource(player.isPlaying()
            ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
    }

    private void startCoverRotate() {
        if (ivCover == null) return;
        if (coverRotate != null) coverRotate.cancel();
        coverRotate = ObjectAnimator.ofFloat(ivCover, "rotation", 0f, 360f);
        coverRotate.setDuration(18000);
        coverRotate.setRepeatCount(ObjectAnimator.INFINITE);
        coverRotate.setInterpolator(new LinearInterpolator());
        coverRotate.start();
    }

    private void stopCoverRotate() {
        if (coverRotate != null) coverRotate.cancel();
    }

    private void bounce(View v) {
        v.animate().scaleX(0.85f).scaleY(0.85f).setDuration(80).start();
        v.postDelayed(new Runnable() {
            @Override public void run() {
                v.animate().scaleX(1f).scaleY(1f).setDuration(240)
                    .setInterpolator(new OvershootInterpolator(2f)).start();
            }
        }, 80);
    }

    private void tick() {
        if (player == null || seek == null) return;
        long pos = player.getCurrentPosition(), dur = player.getDuration();
        if (dur > 0) {
            if (!dragging) { seek.setMax((int) dur); seek.setProgress((int) pos); }
            if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
        }
        if (tvLyric != null && currentIndex < queue.size()) {
            tvLyric.setText("♡ " + queue.get(currentIndex).title);
        }
    }

    private String fmt(int ms) { int s = ms / 1000; return String.format("%d:%02d", s/60, s%60); }

    @Override protected void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (coverRotate != null) coverRotate.cancel();
        super.onDestroy();
    }
}
