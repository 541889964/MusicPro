package com.music.app;

import android.app.Application;
import android.content.Context;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MusicApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        // 全局未捕获异常捕获
        final Thread.UncaughtExceptionHandler def =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override public void uncaughtException(Thread t, Throwable e) {
                try {
                    writeCrash(MusicApp.this, e);
                } catch (Throwable ignored) {}
                if (def != null) def.uncaughtException(t, e);
            }
        });
    }

    private void writeCrash(Context ctx, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println("=== 崩溃时间 ===");
            pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                .format(new Date()));
            pw.println();
            pw.println("=== 异常类型 ===");
            pw.println(e.getClass().getName() + ": " + e.getMessage());
            pw.println();
            pw.println("=== 堆栈 ===");
            e.printStackTrace(pw);
            pw.flush();
            pw.close();

            // 写到多个位置，确保能找到
            String[] paths = {
                "/storage/emulated/0/MT2/crash.txt",
                "/storage/emulated/0/crash.txt",
                "/sdcard/crash.txt",
                ctx.getExternalFilesDir(null) + "/crash.txt"
            };
            for (String p : paths) {
                try {
                    File f = new File(p);
                    File parent = f.getParentFile();
                    if (parent != null && !parent.exists()) parent.mkdirs();
                    FileWriter fw = new FileWriter(f, false);
                    fw.write(sw.toString());
                    fw.close();
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
    }
}
