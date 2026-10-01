package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("mp17", Context.MODE_PRIVATE);
        public static int islandFps(Context c) { return sp(c).getInt("fps", 60); }
    public static void setIslandFps(Context c, int v) { sp(c).edit().putInt("fps", v).apply(); }
}
    public static boolean annShown(Context c) { return sp(c).getBoolean("ann", false); }
    public static void setAnnShown(Context c, boolean v) { sp(c).edit().putBoolean("ann", v).apply(); }
    public static String nickname(Context c) { return sp(c).getString("nick", "亲爱的你"); }
    public static int openCount(Context c) { return sp(c).getInt("cnt", 0); }
    public static void incOpen(Context c) { sp(c).edit().putInt("cnt", openCount(c) + 1).apply(); }
    public static String wallpaper(Context c) { return sp(c).getString("wp", ""); }
    public static void setWallpaper(Context c, String v) { sp(c).edit().putString("wp", v).apply(); }
    public static boolean autoRotate(Context c) { return sp(c).getBoolean("rot", false); }
    public static void setAutoRotate(Context c, boolean v) { sp(c).edit().putBoolean("rot", v).apply(); }
    public static float islandWidth(Context c) { return sp(c).getFloat("iw", 0.42f); }
    public static void setIslandWidth(Context c, float v) { sp(c).edit().putFloat("iw", v).apply(); }
    public static float islandHeight(Context c) { return sp(c).getFloat("ih", 58f); }
    public static void setIslandHeight(Context c, float v) { sp(c).edit().putFloat("ih", v).apply(); }
}
