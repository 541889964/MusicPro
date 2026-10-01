package com.music.app.model;

public class Song {
    public long id;
    public String title;
    public String artist;
    public String album;
    public String path;      // 本地路径
    public String cover;     // 封面 URL
    public String onlineUrl; // 在线播放地址
    public long duration;
    public boolean isOnline; // 是否来自在线搜索

    public String getDurationText() {
        long sec = duration / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
