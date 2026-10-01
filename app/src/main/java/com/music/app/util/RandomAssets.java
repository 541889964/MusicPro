package com.music.app.util;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 从 assets/icons/ 随机取图，用于背景与封面。
 * 内存预算 ≤ 2MB：
 *  - 背景：maxSide 720, RGB_565 → 约 1.0MB
 *  - 封面：maxSide 400, RGB_565 → 约 0.3MB
 *  - 全部静态缓存，同一 Context 不重复解码
 */
public class RandomAssets {

    private static final String DIR = "icons";
    private static final Random RND = new Random();

    private static List<String> cache = null;
    private static Bitmap bgBmp = null;
    private static Bitmap coverBmp = null;

    private static synchronized List<String> list(Context c) {
        if (cache != null) return cache;
        cache = new ArrayList<>();
        try {
            AssetManager am = c.getAssets();
            String[] files = am.list(DIR);
            if (files != null) {
                for (String f : files) {
                    if (f != null && !f.isEmpty()) cache.add(DIR + "/" + f);
                }
            }
        } catch (Throwable ignored) {}
        return cache;
    }

    public static synchronized int count(Context c) { return list(c).size(); }

    private static String pick(Context c) {
        List<String> l = list(c);
        if (l.isEmpty()) return null;
        return l.get(RND.nextInt(l.size()));
    }

    /** 随机背景位图，maxSide 长边像素，RGB_565 省一半内存 */
    public static synchronized Bitmap bg(Context c) {
        if (bgBmp != null && !bgBmp.isRecycled()) return bgBmp;
        String path = pick(c);
        if (path == null) return null;
        bgBmp = decode(c, path, 720);
        return bgBmp;
    }

    /** 随机封面位图（小尺寸） */
    public static synchronized Bitmap cover(Context c) {
        if (coverBmp != null && !coverBmp.isRecycled()) return coverBmp;
        String path = pick(c);
        if (path == null) return null;
        coverBmp = decode(c, path, 400);
        return coverBmp;
    }

    private static Bitmap decode(Context c, String path, int maxSide) {
        try {
            AssetManager am = c.getAssets();
            // 第一遍：读尺寸
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            InputStream is1 = am.open(path);
            BitmapFactory.decodeStream(is1, null, o);
            is1.close();

            int w = o.outWidth, h = o.outHeight;
            if (w <= 0 || h <= 0) return null;

            int sample = 1;
            while (Math.max(w, h) / sample > maxSide * 2) sample *= 2;

            // 第二遍：真解码
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            o2.inPreferredConfig = Bitmap.Config.RGB_565;
            InputStream is2 = am.open(path);
            Bitmap bmp = BitmapFactory.decodeStream(is2, null, o2);
            is2.close();
            if (bmp == null) return null;

            int longer = Math.max(bmp.getWidth(), bmp.getHeight());
            if (longer > maxSide) {
                float s = (float) maxSide / longer;
                Bitmap scaled = Bitmap.createScaledBitmap(bmp,
                    Math.max(1, (int)(bmp.getWidth() * s)),
                    Math.max(1, (int)(bmp.getHeight() * s)), true);
                if (scaled != bmp) bmp.recycle();
                bmp = scaled;
            }
            return bmp;
        } catch (Throwable t) {
            return null;
        }
    }
}
