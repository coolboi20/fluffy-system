package com.example.fluffy;

import java.util.List;

public class PlaylistResponse {
    private String title;
    private String description;
    private List<Song> songs;

    public PlaylistResponse() {}

    public PlaylistResponse(String title, String description, List<Song> songs) {
        this.title = title;
        this.description = description;
        this.songs = songs;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) { this.songs = songs; }

    public static class Song {
        private String title;
        private String artist;

        public Song() {}

        public Song(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
    }
}
