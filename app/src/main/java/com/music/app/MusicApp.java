package com.music.app;
import androidx.multidex.MultiDexApplication;
import com.arthenica.ffmpegkit.FFmpegKitConfig;
public class MusicApp extends MultiDexApplication {
    @Override public void onCreate() {
        super.onCreate();
        FFmpegKitConfig.enableStatisticsCallback(s -> {});
    }
}
