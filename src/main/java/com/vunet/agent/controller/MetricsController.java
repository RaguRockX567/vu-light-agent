package com.vunet.agent.controller;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class MetricsController {

    private static final Logger logger = LoggerFactory.getLogger(MetricsController.class);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${backend.url:http://localhost:8080/api/metrics}")
    private String backendUrl;

    @Value("${backend.apiKey:}")
    private String apiKey;

    // Forward metrics to backend with retries
    @PostMapping("/metrics")
    public ResponseEntity<String> receiveMetrics(@RequestBody(required = false) List<Map<String, Object>> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return ResponseEntity.ok("No metrics to process");
        }

        logger.info("✅ Received metrics batch size={}", metrics.size());
        logger.debug("Metrics payload: {}", metrics);

        // Try to forward to backend with retries
        int maxRetries = 3;
        int retryDelay = 1000; // milliseconds

        for (int i = 1; i <= maxRetries; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // Add API key if configured
                if (apiKey != null && !apiKey.isEmpty()) {
                    headers.set("Authorization", "Bearer " + apiKey);
                }

                HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(metrics, headers);

                final String url = Objects.requireNonNull(backendUrl, "backend.url must not be null");
                ResponseEntity<String> response = restTemplate.postForEntity(
                        url,
                        entity,
                        String.class
                );

                logger.info("📤 Metrics forwarded successfully to backend: status {}", response.getStatusCode());
                return ResponseEntity.ok("Metrics forwarded successfully");

            } catch (Exception e) {
                logger.warn("⚠️ Attempt {}/{} failed to forward metrics: {}",
                        i, maxRetries, e.getMessage());

                if (i == maxRetries) {
                    logger.error("❌ All retry attempts failed", e);
                    return ResponseEntity
                            .status(HttpStatus.BAD_GATEWAY)
                            .body("Failed to forward metrics after " + maxRetries + " retries");
                }

                try {
                    Thread.sleep(retryDelay * i); // exponential backoff
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Should never reach here due to return in final retry
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Unexpected forwarding state");
    }
}
