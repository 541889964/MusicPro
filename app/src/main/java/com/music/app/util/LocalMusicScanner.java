package com.music.app.util;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import java.util.*;
public class LocalMusicScanner {
    public static List<String> scan(Context c) {
        List<String> out = new ArrayList<>();
        try (Cursor cur = c.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{ MediaStore.Audio.Media.DATA},
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null, MediaStore.Audio.Media.TITLE + " ASC")) {
            if (cur != null) while (cur.moveToNext()) {
                String p = cur.getString(0);
                if (p != null) out.add(p);
            }
        } catch (Throwable ignored) {}
        return out;
    }
}
