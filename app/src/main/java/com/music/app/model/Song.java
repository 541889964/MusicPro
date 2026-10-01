package com.music.app.model;

public class Song {
    public long id;
    public String title;
    public String artist;
    public String album;
    public String path;
    public long duration;
    public long albumId;

    public Song() {}

    public String getDurationText() {
        long sec = duration / 1000;
        return String.format("%d:%02d", sec / 60, sec % 60);
    }
}
