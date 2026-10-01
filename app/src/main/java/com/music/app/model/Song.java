package com.music.app.model;

public class Song {
    public long id;
    public String title;
    public String artist;
    public String album;
    public String path;
    public String cover;
    public String onlineUrl;
    public long duration;
    public boolean isOnline;
    public String lyric;
    public String getDurationText() {
        long sec = duration / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
