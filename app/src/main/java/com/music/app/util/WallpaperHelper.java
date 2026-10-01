package com.music.app.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.palette.graphics.Palette;
import java.io.InputStream;
public class WallpaperHelper {
    public static Bitmap loadUser(Context c) {
        try (InputStream in = c.getAssets().open("wallpapers/user_wallpaper.jpg")) {
            return BitmapFactory.decodeStream(in);
        } catch (Throwable e) { return null; }
    }
    public static int dominantColor(Context c) {
        Bitmap bm = loadUser(c);
        if (bm == null) return 0xFF6750A4;
        try {
            Palette p = Palette.from(bm).generate();
            int col = p.getVibrantColor(0);
            if (col == 0) col = p.getDarkVibrantColor(0);
            if (col == 0) col = p.getMutedColor(0xFF6750A4);
            return col;
        } catch (Throwable t) { return 0xFF6750A4; }
    }
}
