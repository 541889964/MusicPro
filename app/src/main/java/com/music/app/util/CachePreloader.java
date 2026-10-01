package com.music.app.util;
import android.content.Context;
import java.io.*;
public class CachePreloader {
    public static void preloadAll(Context ctx) {
        try {
            new File("/storage/emulated/0/sogou/").mkdirs();
            warmupHttp();
            preloadAssets(ctx);
        } catch (Throwable ignored) {}
    }
    private static void warmupHttp() {
        try {
            new okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .build().dispatcher().executorService();
        } catch (Throwable ignored) {}
    }
    private static void preloadAssets(Context c) {
        String[] dirs = {"quotes", "lottie"};
        for (String d : dirs) {
            try {
                String[] fs = c.getAssets().list(d);
                if (fs == null) continue;
                for (String f : fs) {
                    InputStream in = c.getAssets().open(d + "/" + f);
                    byte[] buf = new byte[4096];
                    while (in.read(buf) != -1) {}
                    in.close();
                }
            } catch (Throwable ignored) {}
        }
    }
}
