package com.music.app;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.WindowManager;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.WallpaperHelper;
public class SplashActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_splash);
        ImageView bg = findViewById(R.id.ivWallpaper);
        Bitmap bm = WallpaperHelper.loadCurrent(this);
        if (bm != null && bg != null) bg.setImageBitmap(bm);
        try {
            Intent isl = new Intent(this, com.music.app.service.IslandService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(isl);
            else startService(isl);
            Intent g = new Intent(this, com.music.app.service.GuardService.class);
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(g);
            else startService(g);
        } catch (Throwable ignored) {}
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 800);
    }
}
