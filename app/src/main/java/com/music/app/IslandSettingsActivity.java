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
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
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
        sbCW.setMax(90); sbCW.setProgress(cfg.collapsedW - 10);
        sbCH.setMax(120); sbCH.setProgress(cfg.collapsedH - 30);
        sbEW.setMax(80); sbEW.setProgress(cfg.expandedW - 30);
        sbEH.setMax(400); sbEH.setProgress(cfg.expandedH - 100);
        updateLabels();
        SeekBar.OnSeekBarChangeListener L = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean u) {
                cfg.collapsedW = 10 + sbCW.getProgress();
                cfg.collapsedH = 30 + sbCH.getProgress();
                cfg.expandedW = 30 + sbEW.getProgress();
                cfg.expandedH = 100 + sbEH.getProgress();
                updateLabels();
                cfg.save();
                if (com.music.app.service.IslandService.instance != null)
                    com.music.app.service.IslandService.instance.reloadConfig();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        };
        sbCW.setOnSeekBarChangeListener(L);
        sbCH.setOnSeekBarChangeListener(L);
        sbEW.setOnSeekBarChangeListener(L);
        sbEH.setOnSeekBarChangeListener(L);
        findViewById(R.id.btnReset).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                IslandConfig.reset();
                cfg = new IslandConfig();
                sbCW.setProgress(cfg.collapsedW - 10);
                sbCH.setProgress(cfg.collapsedH - 30);
                sbEW.setProgress(cfg.expandedW - 30);
                sbEH.setProgress(cfg.expandedH - 100);
                updateLabels();
                if (com.music.app.service.IslandService.instance != null)
                    com.music.app.service.IslandService.instance.reloadConfig();
                Toast.makeText(IslandSettingsActivity.this, "已恢复默认", Toast.LENGTH_SHORT).show();
            }
        });
    }
    private void updateLabels() {
        if (tvCW != null) tvCW.setText(cfg.collapsedW + "%");
        if (tvCH != null) tvCH.setText(cfg.collapsedH + "dp");
        if (tvEW != null) tvEW.setText(cfg.expandedW + "%");
        if (tvEH != null) tvEH.setText(cfg.expandedH + "dp");
        if (tvFps != null) tvFps.setText("当前：" + cfg.fps + "fps");
    }
}
