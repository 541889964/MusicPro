package com.music.app;

import android.app.Application;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

public class MusicApp extends Application {
    @Override public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler def =
            Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println("=== " + new Date() + " ===");
                e.printStackTrace(pw);
                pw.close();
                writeSafe(new File(getFilesDir(), "crash.txt"), sw.toString());
                File ext = getExternalFilesDir(null);
                if (ext != null) writeSafe(new File(ext, "crash.txt"), sw.toString());
                writeSafe(new File("/storage/emulated/0/MT2/crash.txt"), sw.toString());
            } catch (Throwable ignored) {}
            if (def != null) def.uncaughtException(t, e);
        });
    }

    private void writeSafe(File f, String s) {
        try {
            if (f.getParentFile() != null && !f.getParentFile().exists())
                f.getParentFile().mkdirs();
            FileWriter fw = new FileWriter(f, false);
            fw.write(s); fw.close();
        } catch (Throwable ignored) {}
    }
}
