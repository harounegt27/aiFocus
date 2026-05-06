package com.example.aiFocus.service;

import com.example.aiFocus.dto.FocusSuggestion;
import com.example.aiFocus.exception.AiServiceException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Gemini AI API to generate focus block suggestions.
 * Handles HTTP requests to the Gemini API, parses responses, and converts them into
 * structured focus suggestions.
 */
@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model}")
    private String model;

    public AiService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public List<FocusSuggestion> getSuggestions(String prompt) {
        try {
            // Build request body — OpenAI format
            Map<String, Object> requestBody = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "user", "content", prompt)
                    ),
                    "temperature", 0.3
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("Content-Type", "application/json");

            HttpEntity<Map<String, Object>> entity =
                    new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl, HttpMethod.POST, entity, String.class);

            // Parse OpenAI-compatible response
            JsonNode root = objectMapper.readTree(response.getBody());
            String text = root
                    .path("choices").get(0)
                    .path("message")
                    .path("content").asText();

            // Strip markdown fences if present
            text = text.replaceAll("```json\\s*", "")
                    .replaceAll("\\s*```", "")
                    .trim();

            return objectMapper.readValue(text,
                    objectMapper.getTypeFactory()
                            .constructCollectionType(List.class, FocusSuggestion.class));

        } catch (Exception e) {
            throw new AiServiceException(
                    "Failed to get suggestions from AI: " + e.getMessage(), e);
        }
    }
}