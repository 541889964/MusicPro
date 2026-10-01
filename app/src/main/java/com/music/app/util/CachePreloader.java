package com.music.app.util;
import android.content.Context;
import java.io.File;
import java.io.InputStream;
public class CachePreloader {
    public static void preloadAll(Context ctx) {
        try {
            File dir = new File("/storage/emulated/0/sogou/");
            if (!dir.exists()) dir.mkdirs();
            preloadAssets(ctx);
        } catch (Throwable ignored) {}
    }
    private static void preloadAssets(Context c) {
        String[] dirs = {"quotes", "lottie"};
        for (int i = 0; i < dirs.length; i++) {
            try {
                String[] fs = c.getAssets().list(dirs[i]);
                if (fs == null) continue;
                for (int j = 0; j < fs.length; j++) {
                    InputStream in = c.getAssets().open(dirs[i] + "/" + fs[j]);
                    byte[] buf = new byte[4096];
                    while (in.read(buf) != -1) {}
                    in.close();
                }
            } catch (Throwable ignored) {}
        }
    }
}
