package com.music.app.util;
import android.content.Context;
import android.content.SharedPreferences;
public class Prefs {
    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("mp", Context.MODE_PRIVATE);
    }
    public static String nickname(Context c) { return sp(c).getString("nick", "亲爱的你"); }
}
