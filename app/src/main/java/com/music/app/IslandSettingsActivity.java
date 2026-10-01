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

    private TextView tvWidthVal, tvHeightVal, tvPreview;
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
        tvPreview = findViewById(R.id.tvPreview);
        sbWidth = findViewById(R.id.sbWidth);
        sbHeight = findViewById(R.id.sbHeight);

        float w = Prefs.islandWidth(this);
        float hh = Prefs.islandHeight(this);
        sbWidth.setMax(60);
        sbWidth.setProgress((int)((w - 0.2f) * 100));
        sbHeight.setMax(80);
        sbHeight.setProgress((int)(hh - 40));

        refreshLabels(w, hh);

        sbWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                float w = 0.2f + p / 100f;
                Prefs.setIslandWidth(IslandSettingsActivity.this, w);
                refreshLabels(w, Prefs.islandHeight(IslandSettingsActivity.this));
                if (fu && IslandService.instance != null) IslandService.instance.applySize();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        sbHeight.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int p, boolean fu) {
                float hh = 40f + p;
                Prefs.setIslandHeight(IslandSettingsActivity.this, hh);
                refreshLabels(Prefs.islandWidth(IslandSettingsActivity.this), hh);
                if (fu && IslandService.instance != null) IslandService.instance.applySize();
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        findViewById(R.id.btnPresetSmall).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applyPreset(0.30f, 44f);
                NiceToast.show(IslandSettingsActivity.this, "已切换小号");
            }
        });
        findViewById(R.id.btnPresetMedium).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applyPreset(0.42f, 58f);
                NiceToast.show(IslandSettingsActivity.this, "已切换中号");
            }
        });
        findViewById(R.id.btnPresetLarge).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                applyPreset(0.58f, 72f);
                NiceToast.show(IslandSettingsActivity.this, "已切换大号");
            }
        });
    }

    private void applyPreset(float w, float hh) {
        Prefs.setIslandWidth(this, w);
        Prefs.setIslandHeight(this, hh);
        sbWidth.setProgress((int)((w - 0.2f) * 100));
        sbHeight.setProgress((int)(hh - 40));
        refreshLabels(w, hh);
        if (IslandService.instance != null) IslandService.instance.applySize();
    }

    private void refreshLabels(float w, float hh) {
        if (tvWidthVal != null)
            tvWidthVal.setText("宽度: " + Math.round(w * 100) + "% 屏幕");
        if (tvHeightVal != null)
            tvHeightVal.setText("高度: " + Math.round(hh) + " dp");
        if (tvPreview != null) {
            int fakeW = Math.round(w * 100);
            tvPreview.setText("预览尺寸: " + fakeW + "% × " + Math.round(hh) + "dp");
        }
    }
}
