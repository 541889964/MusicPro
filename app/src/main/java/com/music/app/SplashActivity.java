package com.music.app;

import android.animation.*;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.view.animation.*;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.music.app.util.CachePreloader;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import com.music.app.widget.ParticleView;
import com.music.app.widget.RippleView;
import com.music.app.widget.RingProgressView;

public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);

        Prefs.incOpen(this);
        new Thread(() -> CachePreloader.preloadAll(getApplicationContext())).start();

        final ImageView bgImg = findViewById(R.id.ivWallpaper);
        Bitmap bm = WallpaperHelper.loadUser(this);
        if (bm != null && bgImg != null) bgImg.setImageBitmap(bm);

        final View root  = findViewById(R.id.splashRoot);
        final View glow  = findViewById(R.id.bgGlow);
        final ImageView logo = findViewById(R.id.ivLogo);
        final LottieAnimationView wave = findViewById(R.id.lottieWave);
        final TextView slogan = findViewById(R.id.tvSlogan);
        final TextView tip = findViewById(R.id.tvTip);
        final RingProgressView ring = findViewById(R.id.progressRing);
        final ParticleView particles = findViewById(R.id.particles);
        if (particles != null) particles.start(10000);

        // 阶段1：光晕呼吸 + Logo 回弹
        ValueAnimator a1 = ValueAnimator.ofFloat(0f, 1f);
        a1.setDuration(1800); a1.setInterpolator(new DecelerateInterpolator());
        a1.addUpdateListener(a -> {
            float v = (float) a.getAnimatedValue();
            glow.setAlpha(0.15f + v * 0.45f);
            glow.setScaleX(0.8f + v * 0.4f);
            glow.setScaleY(0.8f + v * 0.4f);
        });
        a1.start();

        logo.setAlpha(0f); logo.setScaleX(0.4f); logo.setScaleY(0.4f);
        logo.setTranslationY(40f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
            .setStartDelay(200).setDuration(1400)
            .setInterpolator(new OvershootInterpolator(1.6f)).start();

        // 阶段2：涟漪 + 品牌字
        final TextView[] brand = {
            findViewById(R.id.t1), findViewById(R.id.t2), findViewById(R.id.t3),
            findViewById(R.id.t4), findViewById(R.id.t5), findViewById(R.id.t6)
        };
        root.postDelayed(() -> {
            RippleView rv = findViewById(R.id.rippleView);
            if (rv != null) rv.start(2400L);
            for (int i = 0; i < brand.length; i++) {
                TextView tv = brand[i];
                tv.setAlpha(0f); tv.setTranslationY(30f);
                tv.animate().alpha(1f).translationY(0f)
                    .setStartDelay(80L * i).setDuration(600)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
        }, 1600);

        // 阶段3：Lottie 音波
        wave.postDelayed(() -> {
            wave.setVisibility(View.VISIBLE);
            wave.setSpeed(1.15f); wave.playAnimation();
            wave.setAlpha(0f);
            wave.animate().alpha(1f).setDuration(500).start();
        }, 3200);

        slogan.postDelayed(() -> {
            slogan.setAlpha(0f); slogan.setTranslationY(24f);
            slogan.animate().alpha(1f).translationY(0f).setDuration(900)
                .setInterpolator(new DecelerateInterpolator()).start();
        }, 3600);

        // 阶段4：进度环 + 文案
        ring.postDelayed(() -> {
            ring.setVisibility(View.VISIBLE);
            ValueAnimator p = ValueAnimator.ofFloat(0f, 360f);
            p.setDuration(2800);
            p.addUpdateListener(a -> ring.setProgress((float) a.getAnimatedValue()));
            p.start();
        }, 6000);

        String[] tips = {
            "正在为你准备好一切…",
            "正在加载暖心语录…",
            "正在扫描本地音乐…",
            "记得多喝热水哦 💧",
            "天天开心，马上就好 ✨"
        };
        for (int i = 0; i < tips.length; i++) {
            final String t = tips[i];
            root.postDelayed(() -> tip.animate().alpha(0f).setDuration(160)
                .withEndAction(() -> {
                    tip.setText(t);
                    tip.animate().alpha(1f).setDuration(220).start();
                }).start(), 5800L + i * 500L);
        }

        // 阶段5：收束
        root.postDelayed(() -> root.animate().alpha(0f).scaleX(1.05f).scaleY(1.05f)
            .setDuration(900).setInterpolator(new AccelerateInterpolator())
            .withEndAction(() -> {
                Intent it = new Intent(this, MainActivity.class);
                it.putExtra("show_announcement", true);
                startActivity(it);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }).start(), 9100);
    }
}
