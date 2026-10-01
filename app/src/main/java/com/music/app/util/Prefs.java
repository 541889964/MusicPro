package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c) { return c.getSharedPreferences("mp", Context.MODE_PRIVATE); }
    public static boolean annShown(Context c) { return sp(c).getBoolean("ann", false); }
    public static void setAnnShown(Context c, boolean v) { sp(c).edit().putBoolean("ann", v).apply(); }
    public static int cW(Context c) { return sp(c).getInt("cw", 55); }
    public static void setCW(Context c, int v) { sp(c).edit().putInt("cw", v).apply(); }
    public static int cH(Context c) { return sp(c).getInt("ch", 54); }
    public static void setCH(Context c, int v) { sp(c).edit().putInt("ch", v).apply(); }
    public static int eW(Context c) { return sp(c).getInt("ew", 90); }
    public static void setEW(Context c, int v) { sp(c).edit().putInt("ew", v).apply(); }
}
