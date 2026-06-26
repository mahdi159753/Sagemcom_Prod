package com.alibou.security.downtime.prediction;

import com.alibou.security.downtime.Downtime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
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
public class PredictiveAgentClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${predictive.agent.url:http://localhost:8000/predict}")
    private String agentUrl;

    public List<PredictiveInsight> getPredictions(List<Downtime> history) {
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Map Downtime entities to maps for JSON serialization
            List<Map<String, String>> historyData = history.stream().map(d -> {
                Map<String, String> map = new HashMap<>();
                map.put("ligne", d.getLigne());
                map.put("chantier", d.getChantier());
                map.put("severity", d.getSeverity() != null ? d.getSeverity() : "UNKNOWN");
                map.put("type", d.getType() != null ? d.getType() : "UNKNOWN");
                map.put("startTime", d.getStartTime() != null ? d.getStartTime().toString() : "");
                map.put("endTime", d.getEndTime() != null ? d.getEndTime().toString() : null);
                return map;
            }).collect(Collectors.toList());

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("history", historyData);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

            ResponseEntity<List<PredictiveInsight>> response = restTemplate.exchange(
                    agentUrl + "/predict",
                    org.springframework.http.HttpMethod.POST,
                    request,
                    new ParameterizedTypeReference<List<PredictiveInsight>>() {}
            );

            return response.getBody();
        } catch (Exception e) {
            log.error("Failed to fetch predictions from Python agent: ", e);
            return Collections.emptyList();
        }
    }
}
