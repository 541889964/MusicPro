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
        try {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setContentView(R.layout.activity_splash);
            ImageView bg = findViewById(R.id.ivWallpaper);
            Bitmap bm = WallpaperHelper.loadCurrent(this);
            if (bm != null && bg != null) bg.setImageBitmap(bm);
        } catch (Throwable ignored) {}

        // 不在启动页启动服务，避免崩溃。等服务在 MainActivity 启动
        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                try {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                    finish();
                } catch (Throwable ignored) {}
            }
        }, 800);
    }
}
