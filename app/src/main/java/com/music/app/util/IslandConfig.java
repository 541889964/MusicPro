package com.music.app.util;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
public class IslandConfig {
    public static final String FILE = "/storage/emulated/0/Download/island_settings.json";
    public int collapsedW = 55;
    public int collapsedH = 54;
    public int expandedW = 90;
    public int expandedH = 210;
    public int fps = 60;
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
        } catch (Throwable ignored) {}
        return c;
    }
    public void save() {
        try {
            File f = new File(FILE);
            File dir = f.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            JSONObject j = new JSONObject();
            j.put("collapsedW", collapsedW);
            j.put("collapsedH", collapsedH);
            j.put("expandedW", expandedW);
            j.put("expandedH", expandedH);
            j.put("fps", fps);
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
