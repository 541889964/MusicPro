package com.music.app;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private boolean jumped = false;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try { runSplash(); }
        catch (Throwable t) {
            android.util.Log.e("Music", "Splash", t);
            jump();
        }
    }

    private void runSplash() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        final View glow = findViewById(R.id.glow);
        final ImageView logo = findViewById(R.id.logo);
        final TextView brand = findViewById(R.id.brand);
        final TextView tip = findViewById(R.id.tip);
        try { if (tip != null) tip.setText("build " + BuildConfig.BUILD_STAMP); } catch (Throwable ignored) {}

        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1500);
        va.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            glow.setAlpha(0.15f + v * 0.4f);
            glow.setScaleX(0.8f + v * 0.4f);
            glow.setScaleY(0.8f + v * 0.4f);
        });
        va.start();

        logo.setAlpha(0f);
        logo.setScaleX(0.4f);
        logo.setScaleY(0.4f);
        logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(1400)
            .setInterpolator(new OvershootInterpolator(1.6f))
            .start();

        brand.setAlpha(0f);
        brand.setTranslationY(30f);
        brand.animate()
            .alpha(1f).translationY(0f)
            .setStartDelay(400)
            .setDuration(800)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        final String[] tips = {"正在准备…", "加载音乐库…", "扫描本地…", "马上就好…"};
        final Handler h = new Handler(Looper.getMainLooper());
        for (int i = 0; i < tips.length; i++) {
            final String t = tips[i];
            h.postDelayed(() -> {
                tip.animate().alpha(0f).setDuration(150)
                    .withEndAction(() -> {
                        tip.setText(t);
                        tip.animate().alpha(1f).setDuration(200).start();
                    }).start();
            }, 1500L + i * 600L);
        }

        h.postDelayed(this::jump, 4000);
        h.postDelayed(this::jump, 8000);
    }

    private void jump() {
        if (jumped) return;
        jumped = true;
        try {
            startActivity(new Intent(this, MainActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        } catch (Throwable ignored) {}
        finish();
    }
}
