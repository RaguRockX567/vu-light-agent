package com.vunet.agent.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vunet.agent.AgentMain;
import com.vunet.agent.metrics.AgentMetrics;

@RestController
public class HealthController {

    private final AgentMetrics metrics = AgentMetrics.getInstance();

    @Value("${backend.url:http://localhost:8080/api/metrics}")
    private String backendUrl;

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("agentName", "VuNet-Light-Agent");
        response.put("backendUrl", backendUrl);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/metrics")
    public Map<String, Object> metrics() {
        Map<String, Object> data = new HashMap<>();
        data.put("uptimeMs", metrics.getUptimeMs());
        data.put("collected", metrics.getCollectedCount());
        data.put("sent", metrics.getSentCount());
        data.put("dropped", metrics.getDroppedCount());
        data.put("queueSize", AgentMain.getCurrentQueueSize());
        return data;
    }
}
