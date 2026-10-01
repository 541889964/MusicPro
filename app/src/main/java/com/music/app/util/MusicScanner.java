package com.music.app.util;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.provider.MediaStore;
import com.music.app.model.Song;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MusicScanner {

    private static final String[] AUDIO_EXT = {
        ".mp3", ".flac", ".wav", ".m4a", ".aac", ".ogg", ".opus", ".wma", ".ape"
    };

    public static List<Song> scan(Context ctx) {
        List<Song> result = new ArrayList<Song>();
        Set<String> seen = new HashSet<String>();

        // 1. 先走 MediaStore（快）
        scanMediaStore(ctx, result, seen);

        // 2. 再递归扫描所有外部存储目录（全）
        try {
            File root = Environment.getExternalStorageDirectory();
            if (root != null && root.exists()) {
                walk(root, result, seen, 0);
            }
        } catch (Throwable ignored) {}

        return result;
    }

    private static void scanMediaStore(Context ctx, List<Song> out, Set<String> seen) {
        String[] proj = {
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION
        };
        try {
            Cursor c = ctx.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, proj,
                MediaStore.Audio.Media.IS_MUSIC + " != 0", null,
                MediaStore.Audio.Media.TITLE + " ASC");
            if (c != null) {
                int iId = c.getColumnIndex(MediaStore.Audio.Media._ID);
                int iTitle = c.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int iArtist = c.getColumnIndex(MediaStore.Audio.Media.ARTIST);
                int iAlbum = c.getColumnIndex(MediaStore.Audio.Media.ALBUM);
                int iPath = c.getColumnIndex(MediaStore.Audio.Media.DATA);
                int iDur = c.getColumnIndex(MediaStore.Audio.Media.DURATION);
                while (c.moveToNext()) {
                    String path = c.getString(iPath);
                    if (path == null || seen.contains(path)) continue;
                    Song s = new Song();
                    s.id = c.getLong(iId);
                    s.title = c.getString(iTitle);
                    s.artist = c.getString(iArtist);
                    s.album = c.getString(iAlbum);
                    s.path = path;
                    s.duration = c.getLong(iDur);
                    s.isOnline = false;
                    if (s.title == null) s.title = "未知歌曲";
                    if (s.artist == null || s.artist.equals("<unknown>")) s.artist = "未知歌手";
                    if (s.album == null) s.album = "未知专辑";
                    out.add(s);
                    seen.add(path);
                }
                c.close();
            }
        } catch (Throwable ignored) {}
    }

    /** 递归扫描目录 */
    private static void walk(File dir, List<Song> out, Set<String> seen, int depth) {
        if (depth > 12) return; // 防止死循环
        if (dir == null || !dir.exists() || !dir.canRead()) return;
        File[] files;
        try { files = dir.listFiles(); } catch (Throwable t) { return; }
        if (files == null) return;

        for (File f : files) {
            if (f == null) continue;
            String name = f.getName();
            // 跳过隐藏目录和常见系统目录
            if (name.startsWith(".")) continue;
            if (name.equals("Android") && depth == 1) continue;
            if (name.equals("cache") || name.equals("cacheDir")) continue;

            if (f.isDirectory()) {
                walk(f, out, seen, depth + 1);
            } else if (f.isFile()) {
                String lower = name.toLowerCase();
                boolean isAudio = false;
                for (String ext : AUDIO_EXT) {
                    if (lower.endsWith(ext)) { isAudio = true; break; }
                }
                if (!isAudio) continue;
                String path = f.getAbsolutePath();
                if (seen.contains(path)) continue;
                try {
                    Song s = new Song();
                    s.path = path;
                    s.id = path.hashCode();
                    s.isOnline = false;
                    s.duration = 0;
                    s.title = name;
                    int dot = name.lastIndexOf('.');
                    if (dot > 0) s.title = name.substring(0, dot);
                    s.artist = "本地音乐";
                    s.album = dir.getName();
                    // 尝试读取元数据
                    try {
                        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                        mmr.setDataSource(path);
                        String t = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                        String a = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                        String al = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM);
                        String dur = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                        if (t != null && !t.isEmpty()) s.title = t;
                        if (a != null && !a.isEmpty()) s.artist = a;
                        if (al != null && !al.isEmpty()) s.album = al;
                        if (dur != null) {
                            try { s.duration = Long.parseLong(dur); } catch (Throwable ignored) {}
                        }
                        mmr.release();
                    } catch (Throwable ignored) {}
                    out.add(s);
                    seen.add(path);
                } catch (Throwable ignored) {}
            }
        }
    }
}
