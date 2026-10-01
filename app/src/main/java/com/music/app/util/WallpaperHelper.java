package com.music.app.util;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
public class WallpaperHelper {
    public static Bitmap loadUser(Context c) {
        try {
            InputStream in = c.getAssets().open("wallpapers/user_wallpaper.jpg");
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeStream(in, null, o);
            in.close();
            return bm;
        } catch (Throwable e) { return null; }
    }
}
