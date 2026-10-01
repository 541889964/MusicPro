package com.music.app;
import android.app.Application;
public class MusicApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler def = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                java.io.PrintWriter pw = new java.io.PrintWriter(sw);
                pw.println("=== Time: " + new java.util.Date() + " ===");
                e.printStackTrace(pw);
                pw.close();
                java.io.FileWriter fw = new java.io.FileWriter(
                    "/storage/emulated/0/MT2/crash.txt", false);
                fw.write(sw.toString());
                fw.close();
            } catch (Throwable ignored) {}
            if (def != null) def.uncaughtException(t, e);
        });
    }
}
