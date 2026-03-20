package com.example.fluffy;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

/** Main controller for the Fluffy application, handling playlist generation and AI music studio requests. */
@Controller
public class FluffyController {

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private SpotifyService spotifyService;

    @Autowired
    private DeezerService deezerService;

    /** Renders the home page and handles Spotify authentication callbacks. */
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

    /** Handles playlist generation requests from the UI. */
    @PostMapping("/generate")
    public String generate(HttpSession session, Model model, 
                           @RequestParam(value = "mood", required = false) String mood,
                           @RequestParam(value = "image", required = false) MultipartFile image) {
        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null) {
            return "redirect:/";
        }

        try {
            PlaylistResponse playlist;
            if (image != null && !image.isEmpty()) {
                playlist = geminiService.generatePlaylist(mood, image.getBytes(), image.getContentType());
            } else {
                playlist = geminiService.generatePlaylist(mood);
            }
            spotifyService.enrichWithTrackMetadata(accessToken, playlist);
            model.addAttribute("playlist", playlist);
            session.setAttribute("lastPlaylist", playlist);
            session.setAttribute("lastMood", mood);
            model.addAttribute("mood", mood);
        } catch (Exception e) {
            model.addAttribute("error", "Failed to generate playlist: " + e.getMessage());
        }

        return "index";
    }

    /** Handles AI music studio composition requests from the UI. */
    @PostMapping("/generate-music")
    public String generateMusic(HttpSession session, Model model, 
                               @RequestParam(value = "mood", required = false) String mood,
                               @RequestParam(value = "image", required = false) MultipartFile image) {
        try {
            byte[] bytes = (image != null && !image.isEmpty()) ? image.getBytes() : null;
            String contentType = (image != null) ? image.getContentType() : null;
            
            MusicResponse music = geminiService.generateMusicComposition(mood, bytes, contentType);
            
            // Use Deezer fallback to get a 30s audio snippet for the "Reference Track"
            String preview = deezerService.getPreviewUrl(music.getReferenceTrackArtist(), music.getReferenceTrackTitle());
            music.setPreviewUrl(preview);
            
            model.addAttribute("music", music);
            model.addAttribute("mood", mood);
        } catch (Exception e) {
            model.addAttribute("error", "Music Studio error: " + e.getMessage());
        }
        return "index";
    }

    /** Handles AI music studio refinement requests based on a manual prompt. */
    @PostMapping("/generate-music-from-prompt")
    public String generateMusicFromPrompt(HttpSession session, Model model, 
                                        @RequestParam("musicFXPrompt") String prompt,
                                        @RequestParam(value = "mood", required = false) String mood) {
        try {
            MusicResponse music = geminiService.generateMusicFromPrompt(prompt);
        
            // Use Deezer fallback to get a 30s audio snippet for the "Reference Track"
            String preview = deezerService.getPreviewUrl(music.getReferenceTrackArtist(), music.getReferenceTrackTitle());
            music.setPreviewUrl(preview);
        
            model.addAttribute("music", music);
            model.addAttribute("mood", mood);
        } catch (Exception e) {
            model.addAttribute("error", "Music Studio regeneration error: " + e.getMessage());
        }
        return "index";
    }

    /** Handles the final creation of the playlist on the user's Spotify account. */
    @PostMapping("/create-spotify")
    public String createSpotify(HttpSession session, Model model) {
        String accessToken = (String) session.getAttribute("accessToken");
        PlaylistResponse playlist = (PlaylistResponse) session.getAttribute("lastPlaylist");
        String mood = (String) session.getAttribute("lastMood");

        if (accessToken == null || playlist == null) {
            return "redirect:/";
        }

        try {
            String playlistUrl = spotifyService.createPlaylist(accessToken, playlist);
            model.addAttribute("playlistUrl", playlistUrl);
            model.addAttribute("playlist", playlist);
            model.addAttribute("mood", mood);
            model.addAttribute("success", "Playlist created on Spotify!");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to create Spotify playlist: " + e.getMessage());
            model.addAttribute("playlist", playlist);
            model.addAttribute("mood", mood);
        }

        return "index";
    }

    /** Removes a specific song from the generated playlist before creation on Spotify. */
    @PostMapping("/remove-song")
    public String removeSong(HttpSession session, Model model, @RequestParam("index") int index) {
        String accessToken = (String) session.getAttribute("accessToken");
        PlaylistResponse playlist = (PlaylistResponse) session.getAttribute("lastPlaylist");
        String mood = (String) session.getAttribute("lastMood");

        if (accessToken == null || playlist == null) {
            return "redirect:/";
        }

        if (index >= 0 && index < playlist.getSongs().size()) {
            playlist.getSongs().remove(index);
            session.setAttribute("lastPlaylist", playlist);
        }

        model.addAttribute("playlist", playlist);
        model.addAttribute("mood", mood);

        return "index";
    }
}
