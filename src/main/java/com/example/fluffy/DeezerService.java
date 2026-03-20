package com.example.fluffy;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.util.List;
import java.util.Map;

/** Service for interacting with the Deezer API to fetch audio previews. */
@Service
public class DeezerService {
    private final RestClient restClient = RestClient.create();

    /**
     * Fetches a 30-second audio preview URL for a given artist and track title.
     *
     * @param artist The name of the artist.
     * @param title The title of the track.
     * @return A URL to an MP3 preview, or null if not found or an error occurs.
     */
    public String getPreviewUrl(String artist, String title) {
        try {
            // Deezer's public search API
            Map<String, Object> response = restClient.get()
                    .uri("https://api.deezer.com/search?q=artist:\"{artist}\" track:\"{track}\"", artist, title)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("data") instanceof List<?> data && !data.isEmpty()) {
                // Using Java 21+ Sequenced Collections .getFirst()
                Map<String, Object> firstResult = (Map<String, Object>) data.getFirst();
                return (String) firstResult.get("preview");
            }
        } catch (Exception ignored) {
            // Silently fail to keep the app running
        }
        return null;
    }
}
