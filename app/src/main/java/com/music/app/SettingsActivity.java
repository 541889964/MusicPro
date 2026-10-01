package com.music.app;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
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
            safeClick(R.id.itemTestNotif, new Runnable() {
                @Override public void run() { sendTestNotification(); }
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
            Toast.makeText(this, "设置页出错: " + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void sendTestNotification() {
        try {
            String CH = "test_channel";
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                if (nm.getNotificationChannel(CH) == null) {
                    NotificationChannel ch = new NotificationChannel(
                        CH, "测试通知", NotificationManager.IMPORTANCE_HIGH);
                    nm.createNotificationChannel(ch);
                }
            }
            Intent it = new Intent(this, MainActivity.class);
            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;
            PendingIntent pi = PendingIntent.getActivity(this, 0, it, flags);

            androidx.core.app.NotificationCompat.Builder b =
                new androidx.core.app.NotificationCompat.Builder(this, CH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("测试通知")
                .setContentText("这是一条测试通知，灵动岛应该分裂")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pi)
                .setAutoCancel(true);

            if (nm != null) nm.notify(9999, b.build());
            Toast.makeText(this, "已发送测试通知", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "发送失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
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
