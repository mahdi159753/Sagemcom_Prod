package com.alibou.security.nc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NCAgentClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${predictive.agent.url:http://localhost:8000}")
    private String agentBaseUrl;

    private String getAgentUrl() {
        return agentBaseUrl + "/analyze-nc";
    }

    public NCAnalyzeResponse analyze(String description, String localisation, List<NonConformity> history) {
        if (description == null || description.isBlank() || history == null) {
            return new NCAnalyzeResponse("Données insuffisantes", "", "", 0.0);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            List<Map<String, String>> historyData = history.stream().map(nc -> {
                Map<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(nc.getId()));
                map.put("description", nc.getDescription() != null ? nc.getDescription() : "");
                map.put("localisation", nc.getLocalisation() != null ? nc.getLocalisation() : "");
                map.put("actionCorrective", nc.getActionCorrective() != null ? nc.getActionCorrective() : "");
                map.put("assigneeEmail", nc.getAssigneeEmail() != null ? nc.getAssigneeEmail() : "");
                map.put("dateDeclaration", nc.getDateDeclaration() != null ? nc.getDateDeclaration().toString() : null);
                map.put("dateCloture", nc.getDateCloture() != null ? nc.getDateCloture().toString() : null);
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("description", description);
            requestBody.put("localisation", localisation != null ? localisation : "");
            requestBody.put("history", historyData);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<NCAnalyzeResponse> response = restTemplate.postForEntity(
                    getAgentUrl(),
                    request,
                    NCAnalyzeResponse.class
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch NC analysis from Python agent: ", e);
            return new NCAnalyzeResponse("Erreur de connexion à l'agent", "", "", 0.0);
        }
    }
}
