package com.example.fluffy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Service
public class SpotifyService {

    @Value("${SPOTIPY_CLIENT_ID}")
    private String clientId;

    @Value("${SPOTIPY_CLIENT_SECRET}")
    private String clientSecret;

    @Value("${SPOTIPY_REDIRECT_URI}")
    private String redirectUri;

    public SpotifyApi getSpotifyApi(String accessToken) {
        return new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .setRedirectUri(URI.create(redirectUri))
                .setAccessToken(accessToken)
                .build();
    }

    public String createPlaylist(String accessToken, PlaylistResponse playlistData) throws Exception {
        return createPlaylist(getSpotifyApi(accessToken), playlistData);
    }

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
            String query = "track:" + song.getTitle() + " artist:" + song.getArtist();
            Paging<Track> searchResult = spotifyApi.searchTracks(query).limit(1).build().execute();
            if (searchResult.getItems().length > 0) {
                Track track = searchResult.getItems()[0];
                trackUris.add(track.getUri());
                song.setPreviewUrl(track.getPreviewUrl());
                song.setSpotifyUrl(track.getExternalUrls().get("spotify"));
            }
        }

        if (!trackUris.isEmpty()) {
            spotifyApi.addItemsToPlaylist(playlist.getId(), trackUris.toArray(new String[0])).build().execute();
        }

        return playlist.getExternalUrls().get("spotify");
    }

    public void enrichWithTrackMetadata(String accessToken, PlaylistResponse playlist) throws Exception {
        enrichWithTrackMetadata(getSpotifyApi(accessToken), playlist);
    }

    public void enrichWithTrackMetadata(SpotifyApi spotifyApi, PlaylistResponse playlist) throws Exception {
        for (PlaylistResponse.Song song : playlist.getSongs()) {
            String query = "track:" + song.getTitle() + " artist:" + song.getArtist();
            Paging<Track> searchResult = spotifyApi.searchTracks(query).limit(1).build().execute();
            if (searchResult.getItems().length > 0) {
                Track track = searchResult.getItems()[0];
                song.setPreviewUrl(track.getPreviewUrl());
                song.setSpotifyUrl(track.getExternalUrls().get("spotify"));
            }
        }
    }

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
