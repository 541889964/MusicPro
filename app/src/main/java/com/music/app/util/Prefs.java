package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("mp_final", Context.MODE_PRIVATE);
    }
    public static boolean annShown(Context c) { return sp(c).getBoolean("ann", false); }
    public static void setAnnShown(Context c, boolean v) { sp(c).edit().putBoolean("ann", v).apply(); }
    public static String nickname(Context c) { return sp(c).getString("nick", "亲爱的你"); }
    public static int openCount(Context c) { return sp(c).getInt("cnt", 0); }
    public static void incOpen(Context c) { sp(c).edit().putInt("cnt", openCount(c) + 1).apply(); }
    public static String wallpaper(Context c) { return sp(c).getString("wp", ""); }
    public static void setWallpaper(Context c, String v) { sp(c).edit().putString("wp", v).apply(); }
    public static float islandWidth(Context c) { return sp(c).getFloat("iw", 0.55f); }
    public static void setIslandWidth(Context c, float v) { sp(c).edit().putFloat("iw", v).apply(); }
    public static float islandHeight(Context c) { return sp(c).getFloat("ih", 54f); }
    public static void setIslandHeight(Context c, float v) { sp(c).edit().putFloat("ih", v).apply(); }
    public static float islandExpW(Context c) { return sp(c).getFloat("ew", 90f); }
    public static void setIslandExpW(Context c, float v) { sp(c).edit().putFloat("ew", v).apply(); }
}
