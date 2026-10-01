package com.music.app;

import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.model.Song;
import com.music.app.util.MusicScanner;
import com.music.app.util.NiceToast;
import com.music.app.util.WallpaperHelper;
import java.util.*;

public class PlayerActivity extends AppCompatActivity {

    public static ExoPlayer player;
    public static List<Song> queue = new ArrayList<>();
    public static int currentIndex = 0;

    private SeekBar seek;
    private TextView tvTitle, tvArtist, tvCurrent, tvTotal;
    private ImageButton btnPlay, btnPrev, btnNext, btnClose;
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
        btnPlay = findViewById(R.id.btnPlay);
        btnPrev = findViewById(R.id.btnPrev);
        btnNext = findViewById(R.id.btnNext);
        btnClose = findViewById(R.id.btnClose);

        // 拉取队列
        int startIdx = getIntent().getIntExtra("index", 0);
        if (queue.isEmpty() || startIdx != currentIndex) {
            queue = MusicScanner.scan(this);
            currentIndex = Math.max(0, Math.min(startIdx, queue.size() - 1));
        }

        if (queue.isEmpty()) {
            NiceToast.show(this, "播放列表为空");
            finish();
            return;
        }

        initPlayer();
        playAt(currentIndex);

        if (btnPlay != null) btnPlay.setOnClickListener(v -> togglePlay());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) playAt(currentIndex - 1);
        });
        if (btnNext != null) btnNext.setOnClickListener(v -> {
            if (currentIndex < queue.size() - 1) playAt(currentIndex + 1);
        });
        if (btnClose != null) btnClose.setOnClickListener(v -> finish());

        if (seek != null) {
            seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean fromUser) {
                    if (fromUser && tvCurrent != null) {
                        tvCurrent.setText(fmt(p));
                    }
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
                h.postDelayed(this, 500);
            }
        });

        // 入场动画
        View card = findViewById(R.id.playerCard);
        if (card != null) {
            card.setAlpha(0f); card.setTranslationY(80f);
            card.animate().alpha(1f).translationY(0f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
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
        Song song = queue.get(idx);
        if (tvTitle != null) tvTitle.setText(song.title);
        if (tvArtist != null) tvArtist.setText(song.artist);
        if (tvTotal != null) tvTotal.setText(fmt((int) song.duration));
        try {
            MediaItem item = MediaItem.fromUri("file://" + song.path);
            player.setMediaItem(item);
            player.prepare();
            player.play();
        } catch (Throwable t) {
            NiceToast.show(this, "无法播放：" + song.title);
        }
        updatePlayIcon();
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
        if (player == null || seek == null || dragging) return;
        long pos = player.getCurrentPosition();
        long dur = player.getDuration();
        if (dur > 0) {
            seek.setMax((int) dur);
            seek.setProgress((int) pos);
            if (tvCurrent != null) tvCurrent.setText(fmt((int) pos));
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
