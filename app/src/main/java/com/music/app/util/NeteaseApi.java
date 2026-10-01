package com.music.app.util;

import android.os.Handler;
import android.os.Looper;
import com.music.app.model.Song;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NeteaseApi {

    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build();
    private static final Handler UI = new Handler(Looper.getMainLooper());

    public interface OnSearch { void onResult(List<Song> songs); }
    public interface OnLyrics { void onResult(String lrc); }
    public interface OnUrl { void onResult(String url); }

    public static void search(final String keyword, final OnSearch cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                List<Song> list = new ArrayList<Song>();
                try {
                    FormBody body = new FormBody.Builder()
                        .add("s", keyword)
                        .add("type", "1")
                        .add("limit", "30")
                        .add("offset", "0")
                        .build();
                    Request req = new Request.Builder()
                        .url("https://music.163.com/api/search/get/")
                        .addHeader("Referer", "https://music.163.com")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .post(body)
                        .build();
                    Response resp = CLIENT.newCall(req).execute();
                    String json = resp.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONObject result = obj.optJSONObject("result");
                    if (result != null) {
                        JSONArray songs = result.optJSONArray("songs");
                        if (songs != null) {
                            for (int i = 0; i < songs.length(); i++) {
                                JSONObject s = songs.getJSONObject(i);
                                Song song = new Song();
                                song.id = s.optLong("id");
                                song.title = s.optString("name");
                                song.duration = s.optLong("duration");
                                song.isOnline = true;
                                JSONArray artists = s.optJSONArray("artists");
                                StringBuilder sb = new StringBuilder();
                                if (artists != null) {
                                    for (int j = 0; j < artists.length(); j++) {
                                        if (j > 0) sb.append(" / ");
                                        sb.append(artists.getJSONObject(j).optString("name"));
                                    }
                                }
                                song.artist = sb.toString();
                                JSONObject album = s.optJSONObject("album");
                                if (album != null) {
                                    song.album = album.optString("name");
                                    song.cover = album.optString("picUrl");
                                }
                                if (song.album == null || song.album.isEmpty()) song.album = "未知专辑";
                                if (song.artist == null || song.artist.isEmpty()) song.artist = "未知歌手";
                                list.add(song);
                            }
                        }
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                final List<Song> result = list;
                UI.post(new Runnable() {
                    @Override public void run() { cb.onResult(result); }
                });
            }
        }).start();
    }

    public static void getLyrics(final long songId, final OnLyrics cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                String lrc = "";
                try {
                    Request req = new Request.Builder()
                        .url("https://music.163.com/api/song/lyric?id=" + songId + "&lv=1&kv=1&tv=-1")
                        .addHeader("Referer", "https://music.163.com")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .build();
                    Response resp = CLIENT.newCall(req).execute();
                    String json = resp.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONObject lrcObj = obj.optJSONObject("lrc");
                    if (lrcObj != null) lrc = lrcObj.optString("lyric");
                    if (lrc == null || lrc.isEmpty()) {
                        JSONObject tlyric = obj.optJSONObject("tlyric");
                        if (tlyric != null) lrc = tlyric.optString("lyric");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                final String result = lrc == null ? "" : lrc;
                UI.post(new Runnable() {
                    @Override public void run() { cb.onResult(result); }
                });
            }
        }).start();
    }

    public static void getPlayUrl(final long songId, final OnUrl cb) {
        new Thread(new Runnable() {
            @Override public void run() {
                String url = "";
                try {
                    Request req = new Request.Builder()
                        .url("https://music.163.com/api/song/enhance/player/url?id=" + songId
                            + "&ids=[" + songId + "]&br=320000")
                        .addHeader("Referer", "https://music.163.com")
                        .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
                        .build();
                    Response resp = CLIENT.newCall(req).execute();
                    String json = resp.body().string();
                    JSONObject obj = new JSONObject(json);
                    JSONArray data = obj.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        url = data.getJSONObject(0).optString("url");
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }
                if (url == null || url.isEmpty() || url.equals("null")) {
                    url = "https://music.163.com/song/media/outer/url?id=" + songId + ".mp3";
                }
                final String result = url;
                UI.post(new Runnable() {
                    @Override public void run() { cb.onResult(result); }
                });
            }
        }).start();
    }
}
