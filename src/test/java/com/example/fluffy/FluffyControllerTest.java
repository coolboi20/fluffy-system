package com.example.fluffy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.mock.web.MockMultipartFile;
import java.net.URI;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@WebMvcTest(controllers = FluffyController.class)
@org.springframework.test.context.ContextConfiguration(classes = FluffyApplication.class)
class FluffyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeminiService geminiService;

    @MockitoBean
    private SpotifyService spotifyService;

    @Test
    void testIndexWithoutCode() throws Exception {
        when(spotifyService.getAuthorizationUri()).thenReturn(URI.create("http://auth-url"));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("authUrl"))
                .andExpect(model().attribute("authUrl", "http://auth-url"));
    }

    @Test
    void testIndexWithCode() throws Exception {
        when(spotifyService.getAccessToken("test-code")).thenReturn("test-access-token");
        when(spotifyService.getAuthorizationUri()).thenReturn(URI.create("http://auth-url"));

        mockMvc.perform(get("/").param("code", "test-code"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(request().sessionAttribute("accessToken", "test-access-token"));
    }

    @Test
    void testGenerateWithoutAccessToken() throws Exception {
        mockMvc.perform(post("/generate").param("mood", "happy"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }

    @Test
    void testGenerateWithAccessToken() throws Exception {
        PlaylistResponse playlistResponse = new PlaylistResponse();
        playlistResponse.setTitle("Happy Playlist");
        playlistResponse.setSongs(java.util.Collections.emptyList());
        when(geminiService.generatePlaylist("happy")).thenReturn(playlistResponse);

        mockMvc.perform(multipart("/generate")
                        .param("mood", "happy")
                        .sessionAttr("accessToken", "test-access-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlist", playlistResponse))
                .andExpect(request().sessionAttribute("lastPlaylist", playlistResponse))
                .andExpect(request().sessionAttribute("lastMood", "happy"));
        
        org.mockito.Mockito.verify(spotifyService).enrichWithTrackMetadata(eq("test-access-token"), any(PlaylistResponse.class));
    }

    @Test
    void testGenerateWithImage() throws Exception {
        PlaylistResponse playlistResponse = new PlaylistResponse();
        playlistResponse.setTitle("Image Playlist");
        playlistResponse.setSongs(java.util.Collections.emptyList());
        MockMultipartFile imageFile = new MockMultipartFile("image", "test.png", "image/png", "test content".getBytes());
        
        when(geminiService.generatePlaylist(anyString(), any(byte[].class), anyString())).thenReturn(playlistResponse);

        mockMvc.perform(multipart("/generate")
                        .file(imageFile)
                        .param("mood", "vibe")
                        .sessionAttr("accessToken", "test-access-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlist", playlistResponse));
        
        org.mockito.Mockito.verify(spotifyService).enrichWithTrackMetadata(eq("test-access-token"), any(PlaylistResponse.class));
    }

    @Test
    void testCreateSpotifySuccess() throws Exception {
        PlaylistResponse playlistResponse = new PlaylistResponse();
        when(spotifyService.createPlaylist(anyString(), any(PlaylistResponse.class))).thenReturn("http://playlist-url");

        mockMvc.perform(post("/create-spotify")
                        .sessionAttr("accessToken", "test-access-token")
                        .sessionAttr("lastPlaylist", playlistResponse)
                        .sessionAttr("lastMood", "happy"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlistUrl", "http://playlist-url"))
                .andExpect(model().attribute("mood", "happy"))
                .andExpect(model().attribute("success", "Playlist created on Spotify!"));
    }

    @Test
    void testRemoveSongSuccess() throws Exception {
        PlaylistResponse playlistResponse = new PlaylistResponse();
        java.util.List<PlaylistResponse.Song> songs = new java.util.ArrayList<>();
        songs.add(new PlaylistResponse.Song("Song 1", "Artist 1"));
        songs.add(new PlaylistResponse.Song("Song 2", "Artist 2"));
        playlistResponse.setSongs(songs);

        mockMvc.perform(post("/remove-song")
                        .param("index", "0")
                        .sessionAttr("accessToken", "test-access-token")
                        .sessionAttr("lastPlaylist", playlistResponse)
                        .sessionAttr("lastMood", "happy"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlist", playlistResponse))
                .andExpect(model().attribute("mood", "happy"));

        assertEquals(1, playlistResponse.getSongs().size());
        assertEquals("Song 2", playlistResponse.getSongs().get(0).getTitle());
    }

    @Test
    void testRemoveSongWithoutSession() throws Exception {
        mockMvc.perform(post("/remove-song").param("index", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));
    }
}
