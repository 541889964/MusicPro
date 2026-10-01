package com.music.app;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        TextView back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.itemIsland).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, IslandSettingsActivity.class));
            }
        });
        findViewById(R.id.itemTestSplit).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    com.music.app.service.IslandService svc =
                        com.music.app.service.IslandService.instance;
                    if (svc == null) {
                        Toast.makeText(SettingsActivity.this, "请先授权悬浮窗权限", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    svc.testNotifSplit();
                    Toast.makeText(SettingsActivity.this, "分裂动画演示中…", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(SettingsActivity.this, "测试失败", Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.itemNotif).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")); }
                catch (Throwable t) { Toast.makeText(SettingsActivity.this, "请到系统设置开启", Toast.LENGTH_SHORT).show(); }
            }
        });
        findViewById(R.id.itemOverlay).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    try {
                        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        i.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    } catch (Throwable ignored) {}
                }
            }
        });
    }
}
