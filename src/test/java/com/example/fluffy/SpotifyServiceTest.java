package com.example.fluffy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.Playlist;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.model_objects.specification.User;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import se.michaelthelin.spotify.model_objects.specification.ExternalUrl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class SpotifyServiceTest {

    @InjectMocks
    private SpotifyService spotifyService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SpotifyApi spotifyApi;

    @Mock
    private DeezerService deezerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(spotifyService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(spotifyService, "clientSecret", "test-client-secret");
        ReflectionTestUtils.setField(spotifyService, "redirectUri", "http://localhost:8501/");
    }

    @Test
    void testCreatePlaylist() throws Exception {
        // Arrange
        PlaylistResponse playlistData = new PlaylistResponse();
        playlistData.setTitle("Test Playlist");
        playlistData.setDescription("Test Description");
        PlaylistResponse.Song song = new PlaylistResponse.Song();
        song.setTitle("Song 1");
        song.setArtist("Artist 1");
        playlistData.setSongs(Collections.singletonList(song));

        User user = mock(User.class);
        when(user.getId()).thenReturn("user-id");
        when(spotifyApi.getCurrentUsersProfile().build().execute()).thenReturn(user);

        Playlist playlist = mock(Playlist.class);
        when(playlist.getId()).thenReturn("playlist-id");
        ExternalUrl externalUrl = mock(ExternalUrl.class);
        when(externalUrl.get("spotify")).thenReturn("http://playlist-url");
        when(playlist.getExternalUrls()).thenReturn(externalUrl);
        
        when(spotifyApi.createPlaylist(anyString(), anyString())
                .description(anyString())
                .public_(anyBoolean())
                .build()
                .execute()).thenReturn(playlist);

        Track track = mock(Track.class, Answers.RETURNS_DEEP_STUBS);
        when(track.getUri()).thenReturn("track-uri");
        when(track.getPreviewUrl()).thenReturn("http://preview-url");
        ExternalUrl trackExternalUrl = mock(ExternalUrl.class);
        when(trackExternalUrl.get("spotify")).thenReturn("http://track-spotify-url");
        when(track.getExternalUrls()).thenReturn(trackExternalUrl);
        when(track.getAlbum().getImages()).thenReturn(new se.michaelthelin.spotify.model_objects.specification.Image[0]);
        
        Paging<Track> searchResult = mock(Paging.class);
        when(searchResult.getItems()).thenReturn(new Track[]{track});

        when(spotifyApi.searchTracks(anyString())
                .limit(anyInt())
                .build()
                .execute()).thenReturn(searchResult);

        when(spotifyApi.addItemsToPlaylist(anyString(), any(String[].class))
                .build()
                .execute()).thenReturn(null);

        // Act
        String result = spotifyService.createPlaylist(spotifyApi, playlistData);

        // Assert
        assertEquals("http://playlist-url", result);
        assertEquals("http://preview-url", song.getPreviewUrl());
        assertEquals("http://track-spotify-url", song.getSpotifyUrl());
        assertEquals("track-uri", song.getSpotifyUri());
        verify(spotifyApi).createPlaylist("user-id", "Test Playlist");
        verify(spotifyApi).addItemsToPlaylist(eq("playlist-id"), any(String[].class));
    }

    @Test
    void testEnrichWithTrackMetadata() throws Exception {
        // Arrange
        PlaylistResponse playlistData = new PlaylistResponse();
        PlaylistResponse.Song song = new PlaylistResponse.Song();
        song.setTitle("Song 1");
        song.setArtist("Artist 1");
        playlistData.setSongs(Collections.singletonList(song));

        Track track = mock(Track.class, Answers.RETURNS_DEEP_STUBS);
        when(track.getPreviewUrl()).thenReturn("http://preview-url");
        ExternalUrl trackExternalUrl = mock(ExternalUrl.class);
        when(trackExternalUrl.get("spotify")).thenReturn("http://track-spotify-url");
        when(track.getExternalUrls()).thenReturn(trackExternalUrl);
        
        se.michaelthelin.spotify.model_objects.specification.Image image = mock(se.michaelthelin.spotify.model_objects.specification.Image.class);
        when(image.getUrl()).thenReturn("http://image-url");
        when(track.getAlbum().getImages()).thenReturn(new se.michaelthelin.spotify.model_objects.specification.Image[]{image});
        
        Paging<Track> searchResult = mock(Paging.class);
        when(searchResult.getItems()).thenReturn(new Track[]{track});

        when(spotifyApi.searchTracks(anyString())
                .limit(anyInt())
                .build()
                .execute()).thenReturn(searchResult);

        // Act
        spotifyService.enrichWithTrackMetadata(spotifyApi, playlistData);

        // Assert
        assertEquals("http://preview-url", song.getPreviewUrl());
        assertEquals("http://track-spotify-url", song.getSpotifyUrl());
        assertEquals("http://image-url", song.getImageUrl());
        assertNotNull(song.getYoutubeMusicUrl());
    }

    @Test
    void testEnrichWithTrackMetadataDeezerFallback() throws Exception {
        // Arrange
        PlaylistResponse playlistData = new PlaylistResponse();
        PlaylistResponse.Song song = new PlaylistResponse.Song();
        song.setTitle("Song 1");
        song.setArtist("Artist 1");
        playlistData.setSongs(Collections.singletonList(song));

        Track track = mock(Track.class, Answers.RETURNS_DEEP_STUBS);
        when(track.getPreviewUrl()).thenReturn(null); // No Spotify preview
        ExternalUrl trackExternalUrl = mock(ExternalUrl.class);
        when(trackExternalUrl.get("spotify")).thenReturn("http://track-spotify-url");
        when(track.getExternalUrls()).thenReturn(trackExternalUrl);
        when(track.getAlbum().getImages()).thenReturn(new se.michaelthelin.spotify.model_objects.specification.Image[0]);
        
        Paging<Track> searchResult = mock(Paging.class);
        when(searchResult.getItems()).thenReturn(new Track[]{track});

        when(spotifyApi.searchTracks(anyString())
                .limit(anyInt())
                .build()
                .execute()).thenReturn(searchResult);

        when(deezerService.getPreviewUrl("Artist 1", "Song 1")).thenReturn("http://deezer-preview-url");

        // Act
        spotifyService.enrichWithTrackMetadata(spotifyApi, playlistData);

        // Assert
        assertEquals("http://deezer-preview-url", song.getPreviewUrl());
        assertEquals("http://track-spotify-url", song.getSpotifyUrl());
    }

    @Test
    void testEnrichWithTrackMetadataMultipleSongs() throws Exception {
        // Arrange
        PlaylistResponse playlistData = new PlaylistResponse();
        for (int i = 0; i < 15; i++) {
            PlaylistResponse.Song song = new PlaylistResponse.Song();
            song.setTitle("Song " + i);
            song.setArtist("Artist " + i);
            playlistData.getSongs().add(song);
        }

        Track track = mock(Track.class, Answers.RETURNS_DEEP_STUBS);
        when(track.getPreviewUrl()).thenReturn("http://preview-url");
        ExternalUrl trackExternalUrl = mock(ExternalUrl.class);
        when(trackExternalUrl.get("spotify")).thenReturn("http://track-spotify-url");
        when(track.getExternalUrls()).thenReturn(trackExternalUrl);
        when(track.getAlbum().getImages()).thenReturn(new se.michaelthelin.spotify.model_objects.specification.Image[0]);

        Paging<Track> searchResult = mock(Paging.class);
        when(searchResult.getItems()).thenReturn(new Track[]{track});

        when(spotifyApi.searchTracks(anyString())
                .limit(anyInt())
                .build()
                .execute()).thenReturn(searchResult);

        // Act
        spotifyService.enrichWithTrackMetadata(spotifyApi, playlistData);

        // Assert
        for (PlaylistResponse.Song song : playlistData.getSongs()) {
            assertEquals("http://preview-url", song.getPreviewUrl(), "Failed for " + song.getTitle());
            assertEquals("http://track-spotify-url", song.getSpotifyUrl());
            assertNotNull(song.getYoutubeMusicUrl());
        }
    }
}
