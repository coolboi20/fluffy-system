package com.example.fluffy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/** Service for interacting with the Spotify API to create and manage playlists. */
@Service
public class SpotifyService {

    @Value("${SPOTIPY_CLIENT_ID}")
    private String clientId;

    @Value("${SPOTIPY_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${SPOTIPY_REDIRECT_URI}")
    private String redirectUri;

    @Autowired
    private DeezerService deezerService;

    /**
     * Retrieves a SpotifyApi instance configured with the provided access token.
     *
     * @param accessToken The Spotify OAuth access token.
     * @return A configured SpotifyApi instance.
     */
    public SpotifyApi getSpotifyApi(String accessToken) {
        return new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .setAccessToken(accessToken)
                .build();
    }

    /**
     * Creates a new playlist on Spotify for the user.
     *
     * @param accessToken The Spotify OAuth access token.
     * @param playlistData The data for the playlist to be created.
     * @return The external URL of the created playlist.
     */
    public String createPlaylist(String accessToken, PlaylistResponse playlistData) throws Exception {
        return createPlaylist(getSpotifyApi(accessToken), playlistData);
    }

    /**
     * Creates a new playlist on Spotify using a provided SpotifyApi instance.
     *
     * @param spotifyApi The Spotify API client.
     * @param playlistData The data for the playlist to be created.
     * @return The external URL of the created playlist.
     */
    public String createPlaylist(SpotifyApi spotifyApi, PlaylistResponse playlistData) throws Exception {
        User user = spotifyApi.getCurrentUsersProfile().build().execute();
        String userId = user.getId();

        Playlist playlist = spotifyApi.createPlaylist(userId, playlistData.getTitle())
                .description(playlistData.getDescription())
                .public_(true)
                .build()
                .execute();

        List<String> trackUris = new ArrayList<>();
        for (PlaylistResponse.Song song : playlistData.getSongs()) {
            if (song.getSpotifyUri() != null) {
                trackUris.add(song.getSpotifyUri());
            } else {
                // In case it wasn't enriched, search now
                String query = "track:" + song.getTitle() + " artist:" + song.getArtist();
                Paging<Track> searchResult = spotifyApi.searchTracks(query).limit(1).build().execute();
                if (searchResult.getItems().length > 0) {
                    Track track = searchResult.getItems()[0];
                    song.setSpotifyUri(track.getUri());
                    trackUris.add(track.getUri());
                    song.setPreviewUrl(track.getPreviewUrl());
                    song.setSpotifyUrl(track.getExternalUrls().get("spotify"));
                    
                    if (track.getAlbum().getImages().length > 0) {
                        song.setImageUrl(track.getAlbum().getImages()[0].getUrl());
                    }
                }
            }
        }

        if (!trackUris.isEmpty()) {
            spotifyApi.addItemsToPlaylist(playlist.getId(), trackUris.toArray(new String[0])).build().execute();
        }

        return playlist.getExternalUrls().get("spotify");
    }

    /**
     * Enriches a playlist with metadata using an access token.
     *
     * @param accessToken The Spotify OAuth access token.
     * @param playlist The playlist to enrich.
     */
    public void enrichWithTrackMetadata(String accessToken, PlaylistResponse playlist) throws Exception {
        enrichWithTrackMetadata(getSpotifyApi(accessToken), playlist);
    }

    /**
     * Enriches the generated playlist with Spotify metadata and Deezer previews.
     * Uses Java 21+ Virtual Threads to perform enrichment in parallel.
     *
     * @param spotifyApi The Spotify API client.
     * @param playlist The playlist response to enrich.
     */
    public void enrichWithTrackMetadata(SpotifyApi spotifyApi, PlaylistResponse playlist) throws Exception {
        // 1. Pre-generate YouTube Music links (sequential is fine, it's just string formatting)
        for (var song : playlist.getSongs()) {
            try {
                String artist = song.getArtist() != null ? song.getArtist() : "";
                String title = song.getTitle() != null ? song.getTitle() : "";
                String query = artist + " " + title;
                song.setYoutubeMusicUrl("https://music.youtube.com/search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }

        // 2. Parallel enrichment for Spotify/Deezer
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var tasks = playlist.getSongs().stream().map(song -> executor.submit(() -> {
                try {
                    // Small staggered delay to avoid overwhelming APIs
                    Thread.sleep((long) (Math.random() * 300));

                    if (song.getArtist() != null && song.getTitle() != null) {
                        String searchQuery = "track:\"" + song.getTitle() + "\" artist:\"" + song.getArtist() + "\"";
                        
                        Paging<Track> searchResult;
                        // Synchronize on spotifyApi because SpotifyHttpManager's internal HttpClientContext is not thread-safe
                        synchronized (spotifyApi) {
                            searchResult = spotifyApi.searchTracks(searchQuery).limit(1).build().execute();
                        }

                        // If exact match fails, try a looser search
                        if (searchResult.getItems().length == 0) {
                            String looseQuery = song.getArtist() + " " + song.getTitle();
                            synchronized (spotifyApi) {
                                searchResult = spotifyApi.searchTracks(looseQuery).limit(1).build().execute();
                            }
                        }

                        if (searchResult.getItems().length > 0) {
                            Track track = searchResult.getItems()[0];
                            song.setSpotifyUri(track.getUri());
                            song.setPreviewUrl(track.getPreviewUrl());
                            song.setSpotifyUrl(track.getExternalUrls().get("spotify"));

                            if (track.getAlbum().getImages().length > 0) {
                                song.setImageUrl(track.getAlbum().getImages()[0].getUrl());
                            }

                            // Fallback: If Spotify has no preview, ask Deezer
                            if (song.getPreviewUrl() == null) {
                                song.setPreviewUrl(deezerService.getPreviewUrl(song.getArtist(), song.getTitle()));
                            }
                        }
                    }
                } catch (Exception e) {
                    System.err.println("[DEBUG_LOG] Enrichment failed for " + song.getTitle() + ": " + e.getMessage());
                }
            })).toList();

            for (var task : tasks) {
                try {
                    task.get(); // Wait for each task to complete
                } catch (Exception ignored) {
                    // Ignore task failures
                }
            }
        }
    }

    /**
     * Generates the Spotify authorization URI for user login.
     *
     * @return The authorization URI.
     */
    public URI getAuthorizationUri() {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .build();

        return spotifyApi.authorizationCodeUri()
                .scope("playlist-modify-public,playlist-modify-private,user-read-private")
                .show_dialog(false)
                .build()
                .execute();
    }

    /**
     * Exchanges an authorization code for a Spotify access token.
     *
     * @param code The authorization code from Spotify.
     * @return The access token.
     */
    public String getAccessToken(String code) throws Exception {
        SpotifyApi spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .build();

        var credentials = spotifyApi.authorizationCode(code).build().execute();
        return credentials.getAccessToken();
    }
}
