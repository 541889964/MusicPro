package com.music.app;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.Prefs;
import com.music.app.util.WallpaperHelper;
import java.util.List;

public class GalleryActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_gallery);
        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        List<String> all = WallpaperHelper.listSucai(this);
        TextView tvCount = findViewById(R.id.tvCount);
        if (tvCount != null) tvCount.setText("共 " + all.size() + " 张");
        LinearLayout container = findViewById(R.id.llThumbs);
        if (container == null) return;
        int size = (int)(120 * getResources().getDisplayMetrics().density);
        for (int i = 0; i < all.size(); i++) {
            final String name = all.get(i);
            ImageView iv = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(8, 8, 8, 8);
            iv.setLayoutParams(lp);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            Bitmap bm = WallpaperHelper.loadSmall(this, name);
            if (bm != null) iv.setImageBitmap(bm);
            iv.setBackgroundColor(0x22FFFFFF);
            iv.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setWallpaper(GalleryActivity.this, name);
                    com.music.app.util.NiceToast.show(GalleryActivity.this, "已设为背景");
                }
            });
            container.addView(iv);
        }
    }
}
