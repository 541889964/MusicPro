package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c){
        return c.getSharedPreferences("music_pro_warm", Context.MODE_PRIVATE);
    }
    public static boolean announcementShown(Context c){ return sp(c).getBoolean("ann", false); }
    public static void setAnnouncementShown(Context c, boolean v){ sp(c).edit().putBoolean("ann", v).apply(); }
    public static String nickname(Context c){ return sp(c).getString("nick", "亲爱的你"); }
    public static void setNickname(Context c, String v){ sp(c).edit().putString("nick", v).apply(); }
    public static String birthday(Context c){ return sp(c).getString("bday", ""); }
    public static void setBirthday(Context c, String v){ sp(c).edit().putString("bday", v).apply(); }
    public static int openCount(Context c){ return sp(c).getInt("cnt", 0); }
    public static void incOpen(Context c){ sp(c).edit().putInt("cnt", openCount(c) + 1).apply(); }
}
