package com.example.fluffy;

import java.util.List;

/** Response model for AI-generated playlists. */
public class PlaylistResponse {
    private String title;
    private String description;
    private List<Song> songs;

    public PlaylistResponse() {
        this.songs = new java.util.ArrayList<>();
    }

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

    /** Data model for an individual song within a playlist. */
    public static class Song {
        private String title;
        private String artist;
        private String previewUrl;
        private String spotifyUrl;
        private String imageUrl;
        private String youtubeMusicUrl;
        private String spotifyUri;

        public Song() {}

        public Song(String title, String artist) {
            this.title = title;
            this.artist = artist;
        }

        public Song(String title, String artist, String previewUrl) {
            this.title = title;
            this.artist = artist;
            this.previewUrl = previewUrl;
        }

        public Song(String title, String artist, String previewUrl, String spotifyUrl) {
            this.title = title;
            this.artist = artist;
            this.previewUrl = previewUrl;
            this.spotifyUrl = spotifyUrl;
        }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getArtist() { return artist; }
        public void setArtist(String artist) { this.artist = artist; }
        public String getPreviewUrl() { return previewUrl; }
        public void setPreviewUrl(String previewUrl) { this.previewUrl = previewUrl; }
        public String getSpotifyUrl() { return spotifyUrl; }
        public void setSpotifyUrl(String spotifyUrl) { this.spotifyUrl = spotifyUrl; }
        public String getImageUrl() { return imageUrl; }
        public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
        public String getYoutubeMusicUrl() { return youtubeMusicUrl; }
        public void setYoutubeMusicUrl(String youtubeMusicUrl) { this.youtubeMusicUrl = youtubeMusicUrl; }
        public String getSpotifyUri() { return spotifyUri; }
        public void setSpotifyUri(String spotifyUri) { this.spotifyUri = spotifyUri; }
    }
}
