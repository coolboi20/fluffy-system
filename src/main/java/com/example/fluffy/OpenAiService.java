package com.example.fluffy;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.SystemMessage;
import dev.ai4j.openai4j.chat.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAiService {

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaylistResponse generatePlaylist(String mood) throws Exception {
        return generatePlaylist(mood, OpenAiClient.builder().openAiApiKey(apiKey).build());
    }

    public PlaylistResponse generatePlaylist(String mood, OpenAiClient client) throws Exception {
        ChatCompletionRequest request = ChatCompletionRequest.builder()
                .model("gpt-4o")
                .messages(
                        SystemMessage.from("You create short music playlists. Respond strictly in JSON with the keys 'title', 'description' and 'songs'. 'songs' must be a list of objects containing 'title' and 'artist'. Include 10 to 15 songs. Do not add any explanation or extra text."),
                        UserMessage.from("Generate a playlist for the mood: " + mood)
                )
                .temperature(0.7)
                .build();

        ChatCompletionResponse response = client.chatCompletion(request).execute();
        String content = response.choices().get(0).message().content();

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
