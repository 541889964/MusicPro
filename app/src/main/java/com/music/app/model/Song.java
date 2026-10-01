package com.music.app.model;

public class Song {
    public long id;
    public String title = "";
    public String artist = "";
    public String album = "";
    public String path = "";
    public String cover = "";
    public String onlineUrl = "";
    public String lyric = "";
    public long duration;
    public boolean isOnline;

    public String getDur() {
        long s = duration / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }
}
