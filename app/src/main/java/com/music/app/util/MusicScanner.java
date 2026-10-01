package com.music.app.util;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;

public class MusicScanner {
    public static List<Song> lastList = new ArrayList<>();

    public static List<Song> scan(Context c) {
        List<Song> list = new ArrayList<>();
        try {
            Cursor cur = c.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                    MediaStore.Audio.Media._ID,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.ALBUM,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.DURATION
                },
                MediaStore.Audio.Media.IS_MUSIC + " != 0",
                null,
                MediaStore.Audio.Media.TITLE + " ASC");
            if (cur != null) {
                int iId = cur.getColumnIndex(MediaStore.Audio.Media._ID);
                int iT = cur.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int iA = cur.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int iAl = cur.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int iP = cur.getColumnIndex(MediaStore.Audio.Media.DATA);
                int iD = cur.getColumnIndex(MediaStore.Audio.Media.DURATION);
                while (cur.moveToNext()) {
                    Song s = new Song();
                    s.id = cur.getLong(iId);
                    s.title = cur.getString(iT);
                    s.artist = cur.getString(iA);
                    s.album = cur.getString(iAl);
                    s.path = cur.getString(iP);
                    s.duration = cur.getLong(iD);
                    if (s.title == null) s.title = "未知";
                    if (s.artist == null || s.artist.equals("<unknown>")) s.artist = "未知歌手";
                    if (s.album == null) s.album = "未知专辑";
                    if (s.path != null) list.add(s);
                }
                cur.close();
            }
        } catch (Throwable ignored) {}
        lastList = list;
        return list;
    }
}
