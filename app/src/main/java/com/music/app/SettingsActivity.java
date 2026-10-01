package com.music.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.model.Song;
import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);

        ImageButton back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });

        findViewById(R.id.itemIsland).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, IslandSettingsActivity.class));
            }
        });
        findViewById(R.id.itemLock).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (PlayerActivity.queue == null || PlayerActivity.queue.isEmpty()) {
                    android.widget.Toast.makeText(SettingsActivity.this,
                        "先去播放一首歌", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }
                LockScreenActivity.queue = new ArrayList<Song>(PlayerActivity.queue);
                LockScreenActivity.currentIndex = PlayerActivity.currentIndex;
                startActivity(new Intent(SettingsActivity.this, LockScreenActivity.class));
            }
        });
        findViewById(R.id.itemTheme).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, ThemeActivity.class));
            }
        });
        findViewById(R.id.itemGallery).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, GalleryActivity.class));
            }
        });
    }
}
