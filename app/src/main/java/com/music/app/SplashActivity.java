package com.music.app;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.airbnb.lottie.LottieAnimationView;
import com.music.app.util.CachePreloader;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import com.music.app.widget.ParticleView;
import com.music.app.widget.RippleView;
public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        Prefs.incOpen(this);
        new Thread(new Runnable() {
            @Override public void run() {
                CachePreloader.preloadAll(getApplicationContext());
            }
        }).start();
        final ImageView bgImg = findViewById(R.id.ivWallpaper);
        Bitmap bm = WallpaperHelper.loadUser(this);
        if (bm != null && bgImg != null) bgImg.setImageBitmap(bm);
        final View root = findViewById(R.id.splashRoot);
        final View glow = findViewById(R.id.bgGlow);
        final ImageView logo = findViewById(R.id.ivLogo);
        final LottieAnimationView wave = findViewById(R.id.lottieWave);
        final TextView slogan = findViewById(R.id.tvSlogan);
        final TextView tip = findViewById(R.id.tvTip);
        final ParticleView particles = findViewById(R.id.particles);
        if (particles != null) particles.start(10000);
        ValueAnimator va = ValueAnimator.ofFloat(0f, 1f);
        va.setDuration(1800);
        va.setInterpolator(new DecelerateInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                float v = (float) a.getAnimatedValue();
                if (glow != null) {
                    glow.setAlpha(0.15f + v * 0.45f);
                    glow.setScaleX(0.8f + v * 0.4f);
                    glow.setScaleY(0.8f + v * 0.4f);
                }
            }
        });
        va.start();
        if (logo != null) {
            logo.setAlpha(0f);
            logo.setScaleX(0.4f);
            logo.setScaleY(0.4f);
            logo.setTranslationY(40f);
            logo.animate().alpha(1f).scaleX(1f).scaleY(1f).translationY(0f)
                .setStartDelay(200).setDuration(1400)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        }
        final TextView[] brand = new TextView[]{
            findViewById(R.id.t1), findViewById(R.id.t2), findViewById(R.id.t3),
            findViewById(R.id.t4), findViewById(R.id.t5), findViewById(R.id.t6)
        };
        final Handler h = new Handler(Looper.getMainLooper());
        h.postDelayed(new Runnable() {
            @Override public void run() {
                RippleView rv = findViewById(R.id.rippleView);
                if (rv != null) rv.start(2400L);
                for (int i = 0; i < brand.length; i++) {
                    TextView tv = brand[i];
                    if (tv == null) continue;
                    tv.setAlpha(0f);
                    tv.setTranslationY(30f);
                    tv.animate().alpha(1f).translationY(0f)
                        .setStartDelay(80L * i).setDuration(600)
                        .setInterpolator(new DecelerateInterpolator()).start();
                }
            }
        }, 1600);
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (wave == null) return;
                wave.setVisibility(View.VISIBLE);
                wave.setSpeed(1.15f);
                wave.playAnimation();
                wave.setAlpha(0f);
                wave.animate().alpha(1f).setDuration(500).start();
            }
        }, 3200);
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (slogan == null) return;
                slogan.setAlpha(0f);
                slogan.setTranslationY(24f);
                slogan.animate().alpha(1f).translationY(0f).setDuration(900)
                    .setInterpolator(new DecelerateInterpolator()).start();
            }
        }, 3600);
        final String[] tips = {"正在为你准备好一切…","正在加载暖心语录…","正在扫描本地音乐…","记得多喝热水哦 💧","天天开心，马上就好 ✨"};
        for (int i = 0; i < tips.length; i++) {
            final String t = tips[i];
            h.postDelayed(new Runnable() {
                @Override public void run() {
                    if (tip == null) return;
                    tip.animate().alpha(0f).setDuration(160)
                        .withEndAction(new Runnable() {
                            @Override public void run() {
                                tip.setText(t);
                                tip.animate().alpha(1f).setDuration(220).start();
                            }
                        }).start();
                }
            }, 5800L + i * 500L);
        }
        h.postDelayed(new Runnable() {
            @Override public void run() {
                if (root == null) return;
                root.animate().alpha(0f).scaleX(1.05f).scaleY(1.05f)
                    .setDuration(900).setInterpolator(new AccelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override public void onAnimationEnd(Animator a) {
                            Intent it = new Intent(SplashActivity.this, MainActivity.class);
                            it.putExtra("show_announcement", true);
                            startActivity(it);
                            overridePendingTransition(android.R.anim.fade_in,
                                                     android.R.anim.fade_out);
                            finish();
                        }
                    }).start();
            }
        }, 9100);
    }
}
