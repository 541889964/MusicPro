package com.music.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.model.Song;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_settings);

            ImageButton back = findViewById(R.id.btnBack);
            if (back != null) back.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });

            safeClick(R.id.itemIsland, new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(SettingsActivity.this, IslandSettingsActivity.class));
                }
            });
            safeClick(R.id.itemLock, new Runnable() {
                @Override public void run() {
                    if (PlayerActivity.queue == null || PlayerActivity.queue.isEmpty()) {
                        Toast.makeText(SettingsActivity.this, "先去播放一首歌", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    LockScreenActivity.queue = new ArrayList<Song>(PlayerActivity.queue);
                    LockScreenActivity.currentIndex = PlayerActivity.currentIndex;
                    startActivity(new Intent(SettingsActivity.this, LockScreenActivity.class));
                }
            });
            safeClick(R.id.itemTheme, new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(SettingsActivity.this, ThemeActivity.class));
                }
            });
            safeClick(R.id.itemGallery, new Runnable() {
                @Override public void run() {
                    startActivity(new Intent(SettingsActivity.this, GalleryActivity.class));
                }
            });
        } catch (Throwable t) {
            android.util.Log.e("Music", "SettingsActivity fail", t);
            Toast.makeText(this, "设置页出错: " + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void safeClick(int id, final Runnable action) {
        try {
            View v = findViewById(id);
            if (v == null) return;
            v.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { action.run(); }
                    catch (Throwable t) {
                        Toast.makeText(SettingsActivity.this,
                            "打开失败: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (Throwable ignored) {}
    }
}
