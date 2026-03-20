package com.example.fluffy;

import dev.ai4j.openai4j.OpenAiClient;
import dev.ai4j.openai4j.chat.ChatCompletionRequest;
import dev.ai4j.openai4j.chat.ChatCompletionResponse;
import dev.ai4j.openai4j.chat.ChatCompletionChoice;
import dev.ai4j.openai4j.chat.AssistantMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OpenAiServiceTest {

    @InjectMocks
    private OpenAiService openAiService;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private OpenAiClient openAiClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(openAiService, "apiKey", "test-api-key");
    }

    @Test
    void testGeneratePlaylist() throws Exception {
        // Arrange
        String mood = "happy";
        String jsonResponse = "{\"title\":\"Happy Playlist\",\"description\":\"A happy playlist\",\"songs\":[{\"title\":\"Song 1\",\"artist\":\"Artist 1\"}]}";
        
        ChatCompletionResponse response = mock(ChatCompletionResponse.class);
        ChatCompletionChoice choice = mock(ChatCompletionChoice.class);
        AssistantMessage message = mock(AssistantMessage.class);
        
        when(openAiClient.chatCompletion(any(ChatCompletionRequest.class)).execute()).thenReturn(response);
        when(response.choices()).thenReturn(Collections.singletonList(choice));
        when(choice.message()).thenReturn(message);
        when(message.content()).thenReturn(jsonResponse);

        // Act
        PlaylistResponse result = openAiService.generatePlaylist(mood, openAiClient);

        // Assert
        assertNotNull(result);
        assertEquals("Happy Playlist", result.getTitle());
        assertEquals("A happy playlist", result.getDescription());
        assertEquals(1, result.getSongs().size());
        assertEquals("Song 1", result.getSongs().get(0).getTitle());
    }

    @Test
    void testGeneratePlaylistWithMarkdown() throws Exception {
        // Arrange
        String mood = "happy";
        String jsonResponse = "```json\n{\"title\":\"Happy Playlist\",\"description\":\"A happy playlist\",\"songs\":[]}\n```";
        
        ChatCompletionResponse response = mock(ChatCompletionResponse.class);
        ChatCompletionChoice choice = mock(ChatCompletionChoice.class);
        AssistantMessage message = mock(AssistantMessage.class);
        
        when(openAiClient.chatCompletion(any(ChatCompletionRequest.class)).execute()).thenReturn(response);
        when(response.choices()).thenReturn(Collections.singletonList(choice));
        when(choice.message()).thenReturn(message);
        when(message.content()).thenReturn(jsonResponse);

        // Act
        PlaylistResponse result = openAiService.generatePlaylist(mood, openAiClient);

        // Assert
        assertNotNull(result);
        assertEquals("Happy Playlist", result.getTitle());
    }
}
