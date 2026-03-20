package com.example.fluffy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FluffyController.class)
@org.springframework.test.context.ContextConfiguration(classes = FluffyApplication.class)
class FluffyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenAiService openAiService;

    @MockBean
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
        when(openAiService.generatePlaylist("happy")).thenReturn(playlistResponse);

        mockMvc.perform(post("/generate")
                        .param("mood", "happy")
                        .sessionAttr("accessToken", "test-access-token"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlist", playlistResponse))
                .andExpect(request().sessionAttribute("lastPlaylist", playlistResponse));
    }

    @Test
    void testCreateSpotifySuccess() throws Exception {
        PlaylistResponse playlistResponse = new PlaylistResponse();
        when(spotifyService.createPlaylist(anyString(), any(PlaylistResponse.class))).thenReturn("http://playlist-url");

        mockMvc.perform(post("/create-spotify")
                        .sessionAttr("accessToken", "test-access-token")
                        .sessionAttr("lastPlaylist", playlistResponse))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("playlistUrl", "http://playlist-url"))
                .andExpect(model().attribute("success", "Playlist created on Spotify!"));
    }
}
