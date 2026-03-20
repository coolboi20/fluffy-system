package com.example.fluffy;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class FluffyController {

    @Autowired
    private OpenAiService openAiService;

    @Autowired
    private SpotifyService spotifyService;

    @GetMapping("/")
    public String index(HttpSession session, Model model, @RequestParam(value = "code", required = false) String code) {
        if (code != null) {
            try {
                String accessToken = spotifyService.getAccessToken(code);
                session.setAttribute("accessToken", accessToken);
            } catch (Exception e) {
                model.addAttribute("error", "Spotify authentication failed: " + e.getMessage());
            }
        }

        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            model.addAttribute("authUrl", spotifyService.getAuthorizationUri().toString());
        }

        return "index";
    }

    @PostMapping("/generate")
    public String generate(HttpSession session, Model model, @RequestParam("mood") String mood) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            return "redirect:/";
        }

        try {
            PlaylistResponse playlist = openAiService.generatePlaylist(mood);
            model.addAttribute("playlist", playlist);
            session.setAttribute("lastPlaylist", playlist);
            model.addAttribute("mood", mood);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate playlist: " + e.getMessage());
        }

        return "index";
    }

    @PostMapping("/create-spotify")
    public String createSpotify(HttpSession session, Model model) {
        String accessToken = (String) session.getAttribute("accessToken");
        PlaylistResponse playlist = (PlaylistResponse) session.getAttribute("lastPlaylist");

        if (accessToken == null || playlist == null) {
            return "redirect:/";
        }

        try {
            String playlistUrl = spotifyService.createPlaylist(accessToken, playlist);
            model.addAttribute("playlistUrl", playlistUrl);
            model.addAttribute("playlist", playlist);
            model.addAttribute("success", "Playlist created on Spotify!");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create Spotify playlist: " + e.getMessage());
            model.addAttribute("playlist", playlist);
        }

        return "index";
    }
}
