package com.example.fluffy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.Content;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Service for generating music concepts and playlists using Google's Gemini AI model. */
@Service
public class GeminiService {

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Cleans up JSON response from the AI by removing Markdown code fences.
     *
     * @param content The raw content from the AI.
     * @return The cleaned JSON string.
     */
    private String cleanJson(String content) {
        // Clean up content if it has code fences
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            content = content.substring(0, content.lastIndexOf("```"));
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            content = content.substring(0, content.lastIndexOf("```"));
        }
        return content.trim();
    }

    /**
     * Generates a song composition concept using a default Gemini model.
     *
     * @param mood The user's input mood.
     * @param imageBytes Optional image data for multimodal generation.
     * @param mimeType The MIME type of the uploaded image.
     * @return A MusicResponse object containing the generated song concept.
     */
    public MusicResponse generateMusicComposition(String mood, byte[] imageBytes, String mimeType) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not set.");
        }

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3-flash-preview")
                .build();

        return generateMusicComposition(model, mood, imageBytes, mimeType);
    }

    /**
     * Generates a song composition concept using a provided Gemini model.
     *
     * @param model The ChatLanguageModel to use.
     * @param mood The user's input mood.
     * @param imageBytes Optional image data for multimodal generation.
     * @param mimeType The MIME type of the uploaded image.
     * @return A MusicResponse object containing the generated song concept.
     */
    public MusicResponse generateMusicComposition(ChatLanguageModel model, String mood, byte[] imageBytes, String mimeType) throws Exception {
        String systemPrompt = "You are a professional music producer. Generate a song composition concept. " +
                "Respond strictly in JSON with: 'title', 'lyrics' (4-6 lines), 'style' (genre/vibe), " +
                "'musicFXPrompt' (a detailed prompt for an AI music generator optimized for Lyria), " +
                "'referenceTrackArtist' and 'referenceTrackTitle' (a real song that fits this style). " +
                "Do not add any explanation or extra text.";

        List<Content> contents = new ArrayList<>();
        if (mood != null && !mood.isBlank()) {
            contents.add(TextContent.from("Create a song concept based on this mood: " + mood));
        } else if (imageBytes == null) {
            contents.add(TextContent.from("Create a random song concept."));
        } else {
            contents.add(TextContent.from("Create a song concept based on this image."));
        }

        if (imageBytes != null && mimeType != null) {
            contents.add(ImageContent.from(Base64.getEncoder().encodeToString(imageBytes), mimeType));
        }

        try {
            String content = model.generate(SystemMessage.from(systemPrompt), UserMessage.from(contents)).content().text();
            return objectMapper.readValue(cleanJson(content), MusicResponse.class);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    /**
     * Generates a song composition concept based on a manual MusicFX prompt.
     *
     * @param prompt The descriptive music prompt.
     * @return A MusicResponse object containing the generated song concept.
     */
    public MusicResponse generateMusicFromPrompt(String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not set.");
        }

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3-flash-preview")
                .build();

        return generateMusicFromPrompt(model, prompt);
    }

    /**
     * Generates a song composition concept based on a manual MusicFX prompt using a provided model.
     *
     * @param model The ChatLanguageModel to use.
     * @param prompt The descriptive music prompt.
     * @return A MusicResponse object containing the generated song concept.
     */
    public MusicResponse generateMusicFromPrompt(ChatLanguageModel model, String prompt) throws Exception {
        String systemPrompt = "You are a professional music producer. A user has provided a descriptive prompt for an AI music generator. " +
                "Generate a full song composition concept based on this prompt. " +
                "Respond strictly in JSON with: 'title', 'lyrics' (4-6 lines), 'style' (genre/vibe), " +
                "'musicFXPrompt' (use the user's prompt or an improved version of it), " +
                "'referenceTrackArtist' and 'referenceTrackTitle' (a real song that fits this style). " +
                "Do not add any explanation or extra text.";

        try {
            String content = model.generate(SystemMessage.from(systemPrompt), UserMessage.from(prompt)).content().text();
            return objectMapper.readValue(cleanJson(content), MusicResponse.class);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    /**
     * Generates a music playlist based on a mood description.
     *
     * @param mood The user's input mood.
     * @return A PlaylistResponse object containing the generated playlist.
     */
    public PlaylistResponse generatePlaylist(String mood) throws Exception {
        return generatePlaylist(mood, null, null);
    }

    /**
     * Generates a music playlist based on a mood description and optional image.
     *
     * @param mood The user's input mood.
     * @param imageBytes Optional image data.
     * @param mimeType The MIME type of the image.
     * @return A PlaylistResponse object containing the generated playlist.
     */
    public PlaylistResponse generatePlaylist(String mood, byte[] imageBytes, String mimeType) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new RuntimeException("GEMINI_API_KEY is not set. Please define it as an environment variable or in application.properties.");
        }

        ChatLanguageModel model = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName("gemini-3-flash-preview") // Use the correct formal API version string
                .build();

        return generatePlaylist(model, mood, imageBytes, mimeType);
    }

    /**
     * Generates a music playlist based on a mood description using a provided model.
     *
     * @param model The ChatLanguageModel to use.
     * @param mood The user's input mood.
     * @return A PlaylistResponse object containing the generated playlist.
     */
    public PlaylistResponse generatePlaylist(ChatLanguageModel model, String mood) throws Exception {
        return generatePlaylist(model, mood, null, null);
    }

    /**
     * Generates a music playlist based on a mood description and optional image using a provided model.
     *
     * @param model The ChatLanguageModel to use.
     * @param mood The user's input mood.
     * @param imageBytes Optional image data.
     * @param mimeType The MIME type of the image.
     * @return A PlaylistResponse object containing the generated playlist.
     */
    public PlaylistResponse generatePlaylist(ChatLanguageModel model, String mood, byte[] imageBytes, String mimeType) throws Exception {
        String systemPrompt = "You create short music playlists. Respond strictly in JSON with the keys 'title', 'description' and 'songs'. 'songs' must be a list of objects containing 'title' and 'artist'. Include 10 to 15 songs. Do not add any explanation or extra text.";
        
        List<Content> contents = new ArrayList<>();
        if (mood != null && !mood.isBlank()) {
            contents.add(TextContent.from("Generate a playlist for the mood: " + mood));
        } else if (imageBytes == null) {
             contents.add(TextContent.from("Generate a random playlist."));
        } else {
             contents.add(TextContent.from("Generate a playlist based on this image."));
        }
        
        if (imageBytes != null && mimeType != null) {
            contents.add(ImageContent.from(Base64.getEncoder().encodeToString(imageBytes), mimeType));
        }

        try {
            String content = model.generate(SystemMessage.from(systemPrompt), UserMessage.from(contents)).content().text();
            return objectMapper.readValue(cleanJson(content), PlaylistResponse.class);
        } catch (RuntimeException e) {
            handleException(e);
            throw e;
        }
    }

    /**
     * Handles specialized AI model exceptions to provide user-friendly error messages.
     *
     * @param e The caught RuntimeException.
     */
    private void handleException(RuntimeException e) {
        String message = e.getMessage();
        if (message == null) return;
        
        throw switch (message) {
            case String msg when msg.contains("403") && msg.contains("leaked") ->
                new RuntimeException("Google Gemini API Key has been reported as leaked and revoked. Please generate a new key at https://aistudio.google.com/ and set it as an environment variable `GEMINI_API_KEY`.");
            case String msg when msg.contains("400") && msg.contains("expired") ->
                new RuntimeException("Google Gemini API Key has expired. Please renew your key at https://aistudio.google.com/ and update the environment variable `GEMINI_API_KEY`.");
            default -> e;
        };
    }
}
