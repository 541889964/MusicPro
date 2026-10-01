package com.music.app;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.List;
public class ThemeActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_theme);
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        final ImageView preview = findViewById(R.id.ivThemePreview);
        Bitmap cur = WallpaperHelper.loadCurrent(this);
        if (cur != null) preview.setImageBitmap(cur);
        final TextView tvAuto = findViewById(R.id.tvAutoRotate);
        updateAutoText(tvAuto);
        findViewById(R.id.llAutoRotate).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                boolean c = Prefs.autoRotate(ThemeActivity.this);
                Prefs.setAutoRotate(ThemeActivity.this, !c);
                updateAutoText(tvAuto);
                NiceToast.show(ThemeActivity.this, !c ? "已开启自动轮播" : "已关闭自动轮播");
            }
        });
        findViewById(R.id.llRandom).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String name = WallpaperHelper.randomName(ThemeActivity.this);
                if (name != null) {
                    Prefs.setWallpaper(ThemeActivity.this, name);
                    Bitmap bm = WallpaperHelper.load(ThemeActivity.this, name);
                    if (bm != null) {
                        preview.setAlpha(0f);
                        preview.setImageBitmap(bm);
                        preview.animate().alpha(1f).setDuration(500).start();
                    }
                    NiceToast.love(ThemeActivity.this, "已随机换肤");
                }
            }
        });
        findViewById(R.id.llClear).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Prefs.setWallpaper(ThemeActivity.this, "");
                Bitmap bm = WallpaperHelper.loadCurrent(ThemeActivity.this);
                if (bm != null) preview.setImageBitmap(bm);
                NiceToast.show(ThemeActivity.this, "已恢复自动轮播");
            }
        });
        LinearLayout llThumbs = findViewById(R.id.llThumbs);
        List<String> all = WallpaperHelper.listSucai(this);
        int showMax = Math.min(all.size(), 30);
        for (int i = 0; i < showMax; i++) {
            final String name = all.get(i);
            ImageView iv = new ImageView(this);
            int size = (int)(60 * getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(8, 8, 8, 8);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = WallpaperHelper.load(this, name);
            if (bm != null) iv.setImageBitmap(bm);
            iv.setBackgroundColor(0x22FFFFFF);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setWallpaper(ThemeActivity.this, name);
                    Bitmap bm2 = WallpaperHelper.load(ThemeActivity.this, name);
                    if (bm2 != null) {
                        preview.setAlpha(0f);
                        preview.setImageBitmap(bm2);
                        preview.animate().alpha(1f).setDuration(500).start();
                    }
                    NiceToast.love(ThemeActivity.this, "已应用");
                }
            });
            llThumbs.addView(iv);
        }
    }
    private void updateAutoText(TextView tv) {
        if (tv == null) return;
        boolean on = Prefs.autoRotate(this);
        tv.setText(on ? "自动轮播：开启（8 秒/张）" : "自动轮播：关闭");
        tv.setTextColor(on ? Color.parseColor("#FF6B9D") : 0xFFD6CCF5);
    }
}
