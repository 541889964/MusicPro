package com.music.app;

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.util.IslandConfig;

public class IslandSettingsActivity extends AppCompatActivity {
    private IslandConfig cfg;
    private TextView tvCW, tvCH, tvEW, tvEH, tvFps;
    private SeekBar sbCW, sbCH, sbEW, sbEH;
    private final int[] fpsIds = {R.id.fps30, R.id.fps60, R.id.fps90, R.id.fps120, R.id.fps144};
    private final int[] fpsVals = {30, 60, 90, 120, 144};

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        try {
            setContentView(R.layout.activity_island_settings);
            cfg = IslandConfig.load();

            tvCW = findViewById(R.id.tvCW);
            tvCH = findViewById(R.id.tvCH);
            tvEW = findViewById(R.id.tvEW);
            tvEH = findViewById(R.id.tvEH);
            tvFps = findViewById(R.id.tvFps);
            sbCW = findViewById(R.id.sbCW);
            sbCH = findViewById(R.id.sbCH);
            sbEW = findViewById(R.id.sbEW);
            sbEH = findViewById(R.id.sbEH);

            findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { finish(); }
            });

            // 初始化滑块
            sbCW.setProgress(cfg.collapsedW - 10); // 20~90
            sbCH.setProgress(cfg.collapsedH - 30); // 30~90
            sbEW.setProgress(cfg.expandedW - 10);  // 30~90
            sbEH.setProgress(cfg.expandedH - 100);  // 30~210
            updateLabels();

            SeekBar.OnSeekBarChangeListener l = new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                    cfg.collapsedW = 10 + sbCW.getProgress();
                    cfg.collapsedH = 30 + sbCH.getProgress();
                    cfg.expandedW = 10 + sbEW.getProgress();
                    cfg.expandedH = 100 + sbEH.getProgress();
                    updateLabels();
                    cfg.save();
                    notifyService();
                }
                @Override public void onStartTrackingTouch(SeekBar sb) {}
                @Override public void onStopTrackingTouch(SeekBar sb) {}
            };
            sbCW.setOnSeekBarChangeListener(l);
            sbCH.setOnSeekBarChangeListener(l);
            sbEW.setOnSeekBarChangeListener(l);
            sbEH.setOnSeekBarChangeListener(l);

            for (int i = 0; i < fpsIds.length; i++) {
                final int f = fpsVals[i];
                View b = findViewById(fpsIds[i]);
                if (b == null) continue;
                b.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        cfg.fps = f;
                        cfg.save();
                        updateLabels();
                        notifyService();
                        Toast.makeText(IslandSettingsActivity.this,
                            "已切到 " + f + "fps", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            findViewById(R.id.btnReset).setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    IslandConfig.reset();
                    cfg = new IslandConfig();
                    sbCW.setProgress(cfg.collapsedW - 10);
                    sbCH.setProgress(cfg.collapsedH - 30);
                    sbEW.setProgress(cfg.expandedW - 10);
                    sbEH.setProgress(cfg.expandedH - 100);
                    updateLabels();
                    notifyService();
                    Toast.makeText(IslandSettingsActivity.this,
                        "已恢复默认", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Throwable t) {
            Toast.makeText(this, "设置失败: " + t.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void updateLabels() {
        if (tvCW != null) tvCW.setText(cfg.collapsedW + "%");
        if (tvCH != null) tvCH.setText(cfg.collapsedH + "dp");
        if (tvEW != null) tvEW.setText(cfg.expandedW + "%");
        if (tvEH != null) tvEH.setText(cfg.expandedH + "dp");
        if (tvFps != null) tvFps.setText("当前：" + cfg.fps + "fps");
    }

    private void notifyService() {
        try {
            com.music.app.service.IslandService svc = com.music.app.service.IslandService.instance;
            if (svc != null) svc.reloadConfig();
        } catch (Throwable ignored) {}
    }
}
