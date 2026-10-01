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
        int i = idx % all.size();
        if (i < 0) i = -i;
        return all.get(i);
    }

    public static String forSong(Context c, long songId) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        long raw = songId % all.size();
        if (raw < 0) raw = -raw;
        return all.get((int) raw);
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

    public static Bitmap loadAt(Context c, int idx) {
        String name = at(c, idx);
        return load(c, name);
    }

    public static Bitmap loadCurrent(Context c) {
        String name = Prefs.wallpaper(c);
        if (name == null || name.isEmpty()) name = randomName(c);
        return load(c, name);
    }
}
