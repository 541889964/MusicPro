package com.music.app.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WallpaperHelper {

    private static List<String> cache = null;

    public static List<String> listSucai(Context c) {
        if (cache != null) return cache;
        List<String> out = new ArrayList<String>();
        try {
            AssetManager am = c.getAssets();
            String[] fs = am.list("sucai");
            if (fs != null) {
                for (int i = 0; i < fs.length; i++) {
                    String n = fs[i].toLowerCase();
                    if (n.endsWith(".jpg") || n.endsWith(".jpeg")
                        || n.endsWith(".png") || n.endsWith(".webp")) {
                        out.add(fs[i]);
                    }
                }
            }
        } catch (Throwable ignored) {}
        cache = out;
        return out;
    }

    public static String randomName(Context c) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        return all.get(new Random().nextInt(all.size()));
    }

    public static String at(Context c, int idx) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        return all.get(idx % all.size());
    }

    /** 按歌曲 id 稳定选一张（同一首歌每次打开都是同一张封面） */
    public static String forSong(Context c, long songId) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        int idx = (int)(songId % all.size());
        if (idx < 0) idx = -idx;
        return all.get(idx);
    }

    public static Bitmap load(Context c, String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            InputStream in = c.getAssets().open("sucai/" + name);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2;
            Bitmap bm = BitmapFactory.decodeStream(in, null, o);
            in.close();
            return bm;
        } catch (Throwable e) { return null; }
    }

    /** 背景用旧逻辑 */
    public static Bitmap loadCurrent(Context c) {
        String name = Prefs.wallpaper(c);
        if (name == null || name.isEmpty()) name = randomName(c);
        return load(c, name);
    }
}
