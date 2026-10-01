package com.music.app.service;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import com.music.app.LockScreenActivity;
import com.music.app.R;
import com.music.app.model.Song;

public class IslandService extends Service {
    private WindowManager wm;
    private View island;
    private WindowManager.LayoutParams lp;
    private ExoPlayer player;
    private TextView tvTitle, tvArtist;
    private ImageButton btnPlay;
    private boolean expanded = false;
    private int collapsedW, expandedW;
    private final Handler h = new Handler(Looper.getMainLooper());

    @Nullable @Override public IBinder onBind(Intent i) { return null; }

    @Override public void onCreate() {
        super.onCreate();
        player = MusicService.sharedPlayer;
        initView();
        startWatch();
    }

    private void initView() {
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        island = LayoutInflater.from(this).inflate(R.layout.view_island, null);
        int type = Build.VERSION.SDK_INT >= 26
            ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            : WindowManager.LayoutParams.TYPE_PHONE;
        lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = 20;
        wm.addView(island, lp);

        tvTitle = island.findViewById(R.id.islandTitle);
        tvArtist = island.findViewById(R.id.islandArtist);
        btnPlay = island.findViewById(R.id.islandPlay);

        if (btnPlay != null) btnPlay.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player != null) {
                    if (player.isPlaying()) player.pause(); else player.play();
                }
            }
        });
        island.findViewById(R.id.islandRoot).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleExpand(); }
        });
        island.findViewById(R.id.islandNext).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (player != null && player.hasNextMediaItem()) player.seekToNextMediaItem();
            }
        });
        // 入场
        island.setAlpha(0f);
        island.setScaleX(0.6f);
        island.setScaleY(0.6f);
        island.animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(450).setInterpolator(new OvershootInterpolator(1.3f)).start();
    }

    private void toggleExpand() {
        expanded = !expanded;
        float targetScaleX = expanded ? 1f : 1f;
        island.animate()
            .scaleX(targetScaleX).scaleY(targetScaleX)
            .setDuration(300)
            .setInterpolator(new DecelerateInterpolator())
            .start();
        View detail = island.findViewById(R.id.islandDetail);
        if (detail != null) {
            detail.animate().alpha(expanded ? 1f : 0f)
                .setDuration(250).start();
        }
    }

    private void startWatch() {
        h.post(new Runnable() {
            @Override public void run() {
                updateUi();
                h.postDelayed(this, 500);
            }
        });
    }

    private void updateUi() {
        if (player == null) {
            player = MusicService.sharedPlayer;
        }
        if (player == null || tvTitle == null) return;
        try {
            if (player.getCurrentMediaItem() != null && tvArtist != null) {
                tvTitle.setText("♪ 正在播放");
                tvArtist.setText("MUSIC·Pro");
            }
            if (btnPlay != null) {
                btnPlay.setImageResource(player.isPlaying()
                    ? android.R.drawable.ic_media_pause
                    : android.R.drawable.ic_media_play);
            }
        } catch (Throwable ignored) {}
        // 呼吸动画
        island.setScaleX(island.getScaleX());
    }

    @Override public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (island != null && wm != null) {
            try { wm.removeView(island); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }
}
