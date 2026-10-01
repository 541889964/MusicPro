package com.music.app.util;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.LruCache;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
public class WallpaperHelper {
    private static List<String> cache = null;
    private static final LruCache<String, Bitmap> bmpCache = new LruCache<String, Bitmap>(20);
    private static final LruCache<String, Bitmap> smallCache = new LruCache<String, Bitmap>(60);
    public static List<String> listSucai(Context c) {
        if (cache != null) return cache;
        List<String> out = new ArrayList<String>();
        try {
            AssetManager am = c.getAssets();
            String[] fs = am.list("sucai");
            if (fs != null) for (int i = 0; i < fs.length; i++) {
                String n = fs[i].toLowerCase();
                if (n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".png")) out.add(fs[i]);
            }
        } catch (Throwable ignored) {}
        cache = out;
        return out;
    }
    public static String at(Context c, int idx) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        int i = idx % all.size(); if (i < 0) i = -i;
        return all.get(i);
    }
    public static String forSong(Context c, long songId) {
        List<String> all = listSucai(c);
        if (all.isEmpty()) return null;
        long raw = songId % all.size(); if (raw < 0) raw = -raw;
        return all.get((int) raw);
    }
    public static Bitmap load(Context c, String name) {
        if (name == null || name.isEmpty()) return null;
        Bitmap b = bmpCache.get(name); if (b != null) return b;
        try {
            InputStream in = c.getAssets().open("sucai/" + name);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 2; o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bm = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (bm != null) bmpCache.put(name, bm);
            return bm;
        } catch (Throwable e) { return null; }
    }
    public static Bitmap loadSmall(Context c, String name) {
        if (name == null || name.isEmpty()) return null;
        Bitmap b = smallCache.get(name); if (b != null) return b;
        try {
            InputStream in = c.getAssets().open("sucai/" + name);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inSampleSize = 8; o.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bm = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (bm != null) smallCache.put(name, bm);
            return bm;
        } catch (Throwable e) { return null; }
    }
    public static Bitmap loadAt(Context c, int idx) { return load(c, at(c, idx)); }
    public static Bitmap loadCurrent(Context c) {
        String name = Prefs.wallpaper(c);
        if (name == null || name.isEmpty()) name = at(c, 0);
        return load(c, name);
    }
}
