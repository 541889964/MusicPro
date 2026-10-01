package com.music.app.util;

import android.content.Context;
import android.os.Environment;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class IslandConfig {
    public static final String DIR = "/storage/emulated/0/Download/";
    public static final String FILE = DIR + "island_settings.json";

    // 默认值
    public int collapsedW = 55;     // 折叠宽度 % 屏幕
    public int collapsedH = 54;     // 折叠高度 dp
    public int expandedW = 90;      // 展开宽度 %
    public int expandedH = 210;     // 展开高度 dp
    public int fps = 60;            // 帧率
    public boolean showWave = true; // 是否显示声波

    public static IslandConfig load() {
        IslandConfig c = new IslandConfig();
        try {
            File f = new File(FILE);
            if (!f.exists()) return c;
            FileInputStream fis = new FileInputStream(f);
            InputStreamReader r = new InputStreamReader(fis, "UTF-8");
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) != -1) sb.append(buf, 0, n);
            r.close();
            JSONObject j = new JSONObject(sb.toString());
            c.collapsedW = j.optInt("collapsedW", c.collapsedW);
            c.collapsedH = j.optInt("collapsedH", c.collapsedH);
            c.expandedW = j.optInt("expandedW", c.expandedW);
            c.expandedH = j.optInt("expandedH", c.expandedH);
            c.fps = j.optInt("fps", c.fps);
            c.showWave = j.optBoolean("showWave", c.showWave);
        } catch (Throwable ignored) {}
        return c;
    }

    public void save() {
        try {
            File dir = new File(DIR);
            if (!dir.exists()) dir.mkdirs();
            JSONObject j = new JSONObject();
            j.put("collapsedW", collapsedW);
            j.put("collapsedH", collapsedH);
            j.put("expandedW", expandedW);
            j.put("expandedH", expandedH);
            j.put("fps", fps);
            j.put("showWave", showWave);
            FileOutputStream fos = new FileOutputStream(FILE);
            OutputStreamWriter w = new OutputStreamWriter(fos, "UTF-8");
            w.write(j.toString(2));
            w.close();
        } catch (Throwable ignored) {}
    }

    public static void reset() {
        try { new File(FILE).delete(); } catch (Throwable ignored) {}
    }
}
