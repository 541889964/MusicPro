package com.music.app;
import android.os.Build;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
public class SettingsActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView tvCw = findViewById(R.id.tvCw);
        SeekBar sbCw = findViewById(R.id.sbCw);
        TextView tvCh = findViewById(R.id.tvCh);
        SeekBar sbCh = findViewById(R.id.sbCh);
        int cw = com.music.app.util.Prefs.cW(this);
        int ch = com.music.app.util.Prefs.cH(this);
        sbCw.setProgress(cw - 20);
        sbCh.setProgress(ch - 30);
        tvCw.setText(cw + "%");
        tvCh.setText(ch + "dp");
        sbCw.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                int v = p + 20;
                tvCw.setText(v + "%");
                com.music.app.util.Prefs.setCW(SettingsActivity.this, v);
                notifyIsland();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        sbCh.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar sb, int p, boolean u) {
                int v = p + 30;
                tvCh.setText(v + "dp");
                com.music.app.util.Prefs.setCH(SettingsActivity.this, v);
                notifyIsland();
            }
            public void onStartTrackingTouch(SeekBar sb) {}
            public void onStopTrackingTouch(SeekBar sb) {}
        });
        findViewById(R.id.btnTestNotif).setOnClickListener(v -> testNotif());
    }
    private void notifyIsland() {
        try {
            com.music.app.service.IslandService s = com.music.app.service.IslandService.instance;
            if (s != null) s.reloadConfig();
        } catch (Throwable ignored) {}
    }
    private void testNotif() {
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager)
                getSystemService(NOTIFICATION_SERVICE);
            String CH = "test_ch";
            if (Build.VERSION.SDK_INT >= 26 && nm != null) {
                if (nm.getNotificationChannel(CH) == null) {
                    nm.createNotificationChannel(new android.app.NotificationChannel(
                        CH, "测试", android.app.NotificationManager.IMPORTANCE_HIGH));
                }
            }
            androidx.core.app.NotificationCompat.Builder b =
                new androidx.core.app.NotificationCompat.Builder(this, CH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("测试通知")
                .setContentText("灵动岛应该会分裂出通知")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH);
            if (nm != null) nm.notify(999, b.build());
            Toast.makeText(this, "已发送", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            Toast.makeText(this, "发送失败", Toast.LENGTH_SHORT).show();
        }
    }
}
