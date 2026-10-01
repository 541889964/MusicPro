package com.music.app;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.music.app.service.IslandService;
import com.music.app.util.NiceToast;
import com.music.app.util.Prefs;

public class IslandSettingsActivity extends AppCompatActivity {
    private TextView tvWidthVal, tvHeightVal, tvFpsVal;
    private SeekBar sbWidth, sbHeight;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_island_settings);
        ImageButton back = findViewById(R.id.btnBack);
        if (back != null) back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        tvWidthVal = findViewById(R.id.tvWidthVal);
        tvHeightVal = findViewById(R.id.tvHeightVal);
        tvFpsVal = findViewById(R.id.tvFpsVal);
        sbWidth = findViewById(R.id.sbWidth);
        sbHeight = findViewById(R.id.sbHeight);
        float w = Prefs.islandWidth(this);
        float hh = Prefs.islandHeight(this);
        int fps = Prefs.islandFps(this);
        sbWidth.setMax(60); sbWidth.setProgress((int)((w - 0.2f) * 100));
        sbHeight.setMax(80); sbHeight.setProgress((int)(hh - 40));
        refresh(w, hh, fps);

        sbWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                float w = 0.2f + p / 100f;
                Prefs.setIslandWidth(IslandSettingsActivity.this, w);
                refresh(w, Prefs.islandHeight(IslandSettingsActivity.this),
                    Prefs.islandFps(IslandSettingsActivity.this));
                if (fu && IslandService.instance != null) IslandService.instance.applySize();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        sbHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                float hh = 40f + p;
                Prefs.setIslandHeight(IslandSettingsActivity.this, hh);
                refresh(Prefs.islandWidth(IslandSettingsActivity.this), hh,
                    Prefs.islandFps(IslandSettingsActivity.this));
                if (fu && IslandService.instance != null) IslandService.instance.applySize();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
        int[] ids = {R.id.btnFps30, R.id.btnFps60, R.id.btnFps90, R.id.btnFps120, R.id.btnFps144};
        int[] vals = {30, 60, 90, 120, 144};
        for (int i = 0; i < ids.length; i++) {
            final int f = vals[i];
            View b = findViewById(ids[i]);
            if (b == null) continue;
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    Prefs.setIslandFps(IslandSettingsActivity.this, f);
                    refresh(Prefs.islandWidth(IslandSettingsActivity.this),
                        Prefs.islandHeight(IslandSettingsActivity.this), f);
                    if (IslandService.instance != null) IslandService.instance.applyFps();
                    NiceToast.show(IslandSettingsActivity.this, "已切到 " + f + "fps");
                }
            });
        }
    }
    private void refresh(float w, float hh, int fps) {
        if (tvWidthVal != null) tvWidthVal.setText("宽度: " + Math.round(w * 100) + "% 屏幕");
        if (tvHeightVal != null) tvHeightVal.setText("高度: " + Math.round(hh) + " dp");
        if (tvFpsVal != null) tvFpsVal.setText("当前: " + fps + "fps");
    }
}
