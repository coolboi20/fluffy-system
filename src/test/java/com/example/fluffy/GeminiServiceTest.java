package com.example.fluffy;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GeminiServiceTest {

    @InjectMocks
    private GeminiService geminiService;

    @Mock
    private ChatLanguageModel chatLanguageModel;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(geminiService, "apiKey", "test-api-key");
    }

    @Test
    void testGeneratePlaylistSuccess() throws Exception {
        // Arrange
        String mood = "happy";
        String jsonResponse = "```json\n{\"title\": \"Happy Vibes\", \"description\": \"A happy playlist\", \"songs\": [{\"title\": \"Song 1\", \"artist\": \"Artist 1\"}]}\n```";
        
        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);
        
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class))).thenReturn(response);
        
        // Act
        PlaylistResponse result = geminiService.generatePlaylist(chatLanguageModel, mood);
        
        // Assert
        assertNotNull(result);
        assertEquals("Happy Vibes", result.getTitle());
        assertEquals(1, result.getSongs().size());
        assertEquals("Song 1", result.getSongs().get(0).getTitle());
    }

    @Test
    void testGeneratePlaylistWithImage() throws Exception {
        // Arrange
        String mood = "happy";
        byte[] imageBytes = new byte[]{1, 2, 3};
        String mimeType = "image/png";
        String jsonResponse = "{\"title\": \"Image Vibes\", \"description\": \"A playlist from image\", \"songs\": []}";
        
        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);
        
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class))).thenReturn(response);
        
        // Act
        PlaylistResponse result = geminiService.generatePlaylist(chatLanguageModel, mood, imageBytes, mimeType);
        
        // Assert
        assertNotNull(result);
        assertEquals("Image Vibes", result.getTitle());
        verify(chatLanguageModel).generate(any(ChatMessage.class), any(ChatMessage.class));
    }

    @Test
    void testGeneratePlaylistRawJson() throws Exception {
        // Arrange
        String mood = "sad";
        String jsonResponse = "{\"title\": \"Sad Vibes\", \"description\": \"A sad playlist\", \"songs\": []}";
        
        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);
        
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class))).thenReturn(response);
        
        // Act
        PlaylistResponse result = geminiService.generatePlaylist(chatLanguageModel, mood);
        
        // Assert
        assertNotNull(result);
        assertEquals("Sad Vibes", result.getTitle());
    }

    @Test
    void testGenerateMusicCompositionSuccess() throws Exception {
        // Arrange
        String mood = "energetic";
        String jsonResponse = "{\"title\": \"Power Up\", \"lyrics\": \"Line 1\\nLine 2\", \"style\": \"Rock\", \"musicFXPrompt\": \"Rock song\", \"referenceTrackArtist\": \"Artist X\", \"referenceTrackTitle\": \"Song X\"}";
        
        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);
        
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class))).thenReturn(response);
        
        // Act
        MusicResponse result = geminiService.generateMusicComposition(chatLanguageModel, mood, null, null);
        
        // Assert
        assertNotNull(result);
        assertEquals("Power Up", result.getTitle());
        assertEquals("Rock", result.getStyle());
        assertEquals("Artist X", result.getReferenceTrackArtist());
    }

    @Test
    void testGenerateMusicFromPromptSuccess() throws Exception {
        // Arrange
        String prompt = "A heavy metal track with industrial elements";
        String jsonResponse = "{\"title\": \"Metal Machine\", \"lyrics\": \"Steel and iron\\nGrinding gears\", \"style\": \"Industrial Metal\", \"musicFXPrompt\": \"" + prompt + "\", \"referenceTrackArtist\": \"Rammstein\", \"referenceTrackTitle\": \"Du Hast\"}";
        
        AiMessage aiMessage = AiMessage.from(jsonResponse);
        Response<AiMessage> response = Response.from(aiMessage);
        
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class))).thenReturn(response);
        
        // Act
        MusicResponse result = geminiService.generateMusicFromPrompt(chatLanguageModel, prompt);
        
        // Assert
        assertNotNull(result);
        assertEquals("Metal Machine", result.getTitle());
        assertEquals("Industrial Metal", result.getStyle());
        assertEquals("Rammstein", result.getReferenceTrackArtist());
    }

    @Test
    void testHandleLeakedApiKeyError() {
        // Arrange
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenThrow(new RuntimeException("HTTP error (403): { \"error\": { \"code\": 403, \"message\": \"Your API key was reported as leaked. Please use another API key.\", \"status\": \"PERMISSION_DENIED\" } }"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                geminiService.generateMusicComposition(chatLanguageModel, "mood", null, null));
        
        assertTrue(exception.getMessage().contains("reported as leaked and revoked"));
    }

    @Test
    void testHandleExpiredApiKeyError() {
        // Arrange
        when(chatLanguageModel.generate(any(ChatMessage.class), any(ChatMessage.class)))
                .thenThrow(new RuntimeException("HTTP error (400): { \"error\": { \"code\": 400, \"message\": \"API key expired. Please renew the API key.\", \"status\": \"INVALID_ARGUMENT\" } }"));
        
        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
                geminiService.generateMusicComposition(chatLanguageModel, "mood", null, null));
        
        assertTrue(exception.getMessage().contains("has expired"));
    }
}
