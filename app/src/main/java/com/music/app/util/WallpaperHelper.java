package com.music.app.util;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;

public class WallpaperHelper {
    public static Bitmap generateCircleCover(int size) {
        try {
            Bitmap bm = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(bm);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            LinearGradient lg = new LinearGradient(0, 0, size, size,
                new int[]{0xFF9B6BFF, 0xFFFF6B9D},
                null, Shader.TileMode.CLAMP);
            p.setShader(lg);
            cv.drawCircle(size / 2f, size / 2f, size / 2f, p);
            return bm;
        } catch (Throwable t) {
            return null;
        }
    }
}
