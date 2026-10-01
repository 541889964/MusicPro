package com.music.app.util;
import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import com.music.app.model.Song;
import java.util.ArrayList;
import java.util.List;
public class MusicScanner {
    public static List<Song> scan(Context ctx) {
        List<Song> list = new ArrayList<Song>();
        String[] proj = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION};
        try {
            Cursor c = ctx.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                MediaStore.Audio.Media.TITLE + " ASC");
            if (c != null) {
                int iId = c.getColumnIndex(MediaStore.Audio.Media._ID);
                int iT = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int iA = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int iAl = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int iP = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                int iD = c.getColumnIndex(MediaStore.Audio.Media.DURATION);
                while (c.moveToNext()) {
                    Song s = new Song();
                    s.id = c.getLong(iId);
                    s.title = c.getString(iT);
                    s.artist = c.getString(iA);
                    s.album = c.getString(iAl);
                    s.path = c.getString(iP);
                    s.duration = c.getLong(iD);
                    s.isOnline = false;
                    if (s.title == null) s.title = "未知歌曲";
                    if (s.artist == null || s.artist.equals("<unknown>")) s.artist = "未知歌手";
                    if (s.album == null) s.album = "未知专辑";
                    if (s.path != null) list.add(s);
                }
                c.close();
            }
        } catch (Throwable ignored) {}
        return list;
    }
}
