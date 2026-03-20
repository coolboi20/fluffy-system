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

@Service
public class GeminiService {

    @Value("${GEMINI_API_KEY:}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaylistResponse generatePlaylist(String mood) throws Exception {
        return generatePlaylist(mood, null, null);
    }

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

    public PlaylistResponse generatePlaylist(ChatLanguageModel model, String mood) throws Exception {
        return generatePlaylist(model, mood, null, null);
    }

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

        String content = model.generate(SystemMessage.from(systemPrompt), UserMessage.from(contents)).content().text();

        // Clean up content if it has code fences
        if (content.contains("```json")) {
            content = content.substring(content.indexOf("```json") + 7);
            content = content.substring(0, content.lastIndexOf("```"));
        } else if (content.contains("```")) {
            content = content.substring(content.indexOf("```") + 3);
            content = content.substring(0, content.lastIndexOf("```"));
        }
        
        content = content.trim();

        return objectMapper.readValue(content, PlaylistResponse.class);
    }
}
