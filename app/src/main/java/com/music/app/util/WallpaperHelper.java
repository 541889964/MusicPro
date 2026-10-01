package com.music.app.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
public class WallpaperHelper {
    public static Bitmap loadUser(Context c) {
        try (InputStream in = c.getAssets().open("wallpapers/user_wallpaper.jpg")) {
            return BitmapFactory.decodeStream(in);
        } catch (Throwable e) { return null; }
    }
}
