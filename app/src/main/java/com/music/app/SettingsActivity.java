package com.music.app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.NiceToast;

public class SettingsActivity extends AppCompatActivity {
    private void safeSetClick(int id, View.OnClickListener l) {
        try { View v = findViewById(id); if (v != null) v.setOnClickListener(l); }
        catch (Throwable ignored) {}
    }
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_settings);
        safeSetClick(R.id.btnBack,new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        safeSetClick(R.id.btnGallery,new View.OnClickListener() {
            @Override public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, GalleryActivity.class));
            }
        });
        safeSetClick(R.id.btnOverlay,new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 23) {
                    try {
                        Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        i.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(i);
                    } catch (Throwable ignored) {}
                }
            }
        });
        safeSetClick(R.id.btnNotif,new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= 22) {
                    try {
                        startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                    } catch (Throwable ignored) {}
                }
            }
        });
        final TextView tvAlarmText = findViewById(R.id.tvAlarmText);
        if (tvAlarmText != null) tvAlarmText.setText(com.music.app.util.AlarmHelper.getText(this));

        safeSetClick(R.id.btnNotifTest,new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Class<?> cls = Class.forName("com.music.app.service.IslandService");
                    cls.getMethod("notify", String.class, String.class)
                        .invoke(null, "🔔 测试通知", "如果你看到这条，说明通知栏正常");
                    NiceToast.show(SettingsActivity.this, "已发送测试通知");
                } catch (Throwable t) {
                    NiceToast.show(SettingsActivity.this, "发送失败");
                }
            }
        });

        safeSetClick(R.id.btnAlarm,new View.OnClickListener() {
            @Override public void onClick(View v) {
                java.util.Calendar c = java.util.Calendar.getInstance();
                new android.app.TimePickerDialog(SettingsActivity.this,
                    new android.app.TimePickerDialog.OnTimeSetListener() {
                        @Override public void onTimeSet(android.widget.TimePicker tp, int h, int m) {
                            com.music.app.util.AlarmHelper.set(SettingsActivity.this, h, m);
                            if (tvAlarmText != null)
                                tvAlarmText.setText(com.music.app.util.AlarmHelper.getText(SettingsActivity.this));
                            NiceToast.show(SettingsActivity.this, "闹钟已设置 " + String.format("%02d:%02d", h, m));
                        }
                    }, c.get(java.util.Calendar.HOUR_OF_DAY), c.get(java.util.Calendar.MINUTE), true).show();
            }
        });

        safeSetClick(R.id.btnBattery,new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    Intent i = new Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                    startActivity(i);
                } catch (Throwable ignored) {}
            }
        });
        safeSetClick(R.id.btnAbout,new View.OnClickListener() {
            @Override public void onClick(View v) {
                NiceToast.show(SettingsActivity.this, "拾音 v10.0");
            }
        });
    }
}
