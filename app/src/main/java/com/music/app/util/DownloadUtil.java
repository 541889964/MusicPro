package com.music.app.util;

import android.os.Handler;
import android.os.Looper;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadUtil {
    public static final String DIR = "/storage/emulated/0/sogou/";

    public interface Callback {
        void onDone(boolean ok, String path);
    }

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    public static void download(final String url, final String filename, final Callback cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                boolean ok = false;
                String msg = "";
                try {
                    File dir = new File(DIR);
                    if (!dir.exists()) dir.mkdirs();
                    Request req = new Request.Builder()
                        .url(url)
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .addHeader("Referer", "https://music.163.com")
                        .build();
                    Response resp = CLIENT.newCall(req).execute();
                    if (!resp.isSuccessful()) {
                        msg = "HTTP " + resp.code();
                    } else {
                        File f = new File(dir, filename);
                        FileOutputStream fos = new FileOutputStream(f);
                        InputStream is = resp.body().byteStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                        fos.close();
                        is.close();
                        if (f.length() > 0) { ok = true; msg = f.getAbsolutePath(); }
                        else { msg = "文件为空"; f.delete(); }
                    }
                } catch (Throwable t) {
                    msg = t.getMessage();
                }
                final boolean fOk = ok;
                final String fMsg = msg;
                UI.post(new Runnable() {
                    @Override public void run() { cb.onDone(fOk, fMsg); }
                });
            }
        }).start();
    }

    public static void saveLyrics(final String lrc, final String filename, final Callback cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                boolean ok = false;
                String msg = "";
                try {
                    File dir = new File(DIR);
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, filename);
                    FileOutputStream fos = new FileOutputStream(f);
                    fos.write(lrc.getBytes("UTF-8"));
                    fos.close();
                    ok = true;
                    msg = f.getAbsolutePath();
                } catch (Throwable t) {
                    msg = t.getMessage();
                }
                final boolean fOk = ok;
                final String fMsg = msg;
                UI.post(new Runnable() {
                    @Override public void run() { cb.onDone(fOk, fMsg); }
                });
            }
        }).start();
    }
}
