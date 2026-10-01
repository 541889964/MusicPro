package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("mp9", Context.MODE_PRIVATE);
    }
    public static boolean annShown(Context c) { return sp(c).getBoolean("ann", false); }
    public static void setAnnShown(Context c, boolean v) { sp(c).edit().putBoolean("ann", v).apply(); }
    public static String nickname(Context c) { return sp(c).getString("nick", "亲爱的你"); }
    public static int openCount(Context c) { return sp(c).getInt("cnt", 0); }
    public static void incOpen(Context c) { sp(c).edit().putInt("cnt", openCount(c) + 1).apply(); }
}
