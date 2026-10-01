package com.music.app.util;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
public class WallpaperHelper {
    public static Bitmap generateWallpaper(Context c) {
        try {
            int w = 1080, h = 1920;
            Bitmap bm = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            Canvas cv = new Canvas(bm);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            LinearGradient lg = new LinearGradient(0, 0, w, h,
                new int[]{0xFF0D0B1F, 0xFF221047, 0xFF6750A4, 0xFFFF6B9D, 0xFF0D0B1F},
                null, android.graphics.Shader.TileMode.CLAMP);
            p.setShader(lg);
            cv.drawRect(0, 0, w, h, p);
            p.setShader(null);
            return bm;
        } catch (Throwable t) { return null; }
    }
    public static Bitmap generateCircleCover(int size) {
        try {
            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            LinearGradient lg = new LinearGradient(0, 0, size, size,
                new int[]{0xFF9B6BFF, 0xFFFF6B9D}, null, android.graphics.Shader.TileMode.CLAMP);
            p.setShader(lg);
            cv.drawCircle(size/2f, size/2f, size/2f, p);
            return bm;
        } catch (Throwable t) { return null; }
    }
}
