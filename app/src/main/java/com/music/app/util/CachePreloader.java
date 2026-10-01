package com.music.app.util;
import android.content.Context;
import java.io.*;
public class CachePreloader {
    public static void preloadAll(Context ctx) {
        try {
            new File("/storage/emulated/0/sogou/").mkdirs();
            LocalMusicScanner.scan(ctx);
            warmup();
            preloadAssets(ctx);
        } catch (Throwable ignored) {}
    }
    private static void warmup() {
        try {
            new okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .connectionPool(new okhttp3.ConnectionPool(5, 5,
                    java.util.concurrent.TimeUnit.MINUTES))
                .build().dispatcher().executorService();
        } catch (Throwable ignored) {}
    }
    private static void preloadAssets(Context c) {
        String[] dirs = {"presets", "quotes", "lottie", "stickers"};
        for (String d : dirs) {
            try {
                String[] fs = c.getAssets().list(d);
                if (fs == null) continue;
                for (String f : fs) {
                    try (InputStream in = c.getAssets().open(d + "/" + f)) {
                        byte[] b = new byte[4096];
                        while (in.read(b) != -1) {}
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
        }
    }
}
