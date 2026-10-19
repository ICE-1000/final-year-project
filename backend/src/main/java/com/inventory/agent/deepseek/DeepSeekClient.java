package com.inventory.agent.deepseek;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DeepSeekClient {
    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.api-url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String getModel() {
        return model;
    }

    public ChatCompletionResponse complete(ChatCompletionRequest request) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        HttpEntity<ChatCompletionRequest> entity = new HttpEntity<>(request, headers);
        ResponseEntity<ChatCompletionResponse> response =
                restTemplate.postForEntity(apiUrl, entity, ChatCompletionResponse.class);
        return response.getBody();
    }
}
