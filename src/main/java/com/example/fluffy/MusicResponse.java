package com.example.fluffy;

/** Response model for AI-generated music compositions. */
public class MusicResponse {
    private String title;
    private String lyrics;
    private String style;
    private String musicFXPrompt;
    private String referenceTrackArtist;
    private String referenceTrackTitle;
    private String previewUrl; // Audio snippet from Deezer/Spotify

    public MusicResponse() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getLyrics() { return lyrics; }
    public void setLyrics(String lyrics) { this.lyrics = lyrics; }
    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }
    public String getMusicFXPrompt() { return musicFXPrompt; }
    public void setMusicFXPrompt(String musicFXPrompt) { this.musicFXPrompt = musicFXPrompt; }
    public String getReferenceTrackArtist() { return referenceTrackArtist; }
    public void setReferenceTrackArtist(String rta) { this.referenceTrackArtist = rta; }
    public String getReferenceTrackTitle() { return referenceTrackTitle; }
    public void setReferenceTrackTitle(String rtt) { this.referenceTrackTitle = rtt; }
    public String getPreviewUrl() { return previewUrl; }
    public void setPreviewUrl(String url) { this.previewUrl = url; }
}
