package com.alibou.security.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AICopilotClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${predictive.agent.url:http://localhost:8000}")
    private String agentBaseUrl;

    private String getCopilotUrl() {
        return agentBaseUrl + "/chat-copilot";
    }

    public String askCopilot(String message, Integer userId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("message", message);
            requestBody.put("user_id", userId);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    getCopilotUrl(),
                    request,
                    Map.class
            );

            if (response.getBody() != null && response.getBody().containsKey("reply")) {
                return (String) response.getBody().get("reply");
            }
            return "Désolé, je n'ai pas pu générer une réponse.";
        } catch (Exception e) {
            log.error("Failed to fetch response from Copilot AI: ", e);
            return "Erreur de connexion à l'Agent Copilot IA.";
        }
    }
}
