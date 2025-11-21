package com.vunet.agent;

import java.io.File;
import java.io.FileReader;

import com.google.gson.Gson;

/**
 * Simple config holder. Fields are encapsulated (private) with getters.
 */
public class AgentConfig {
    private String agentId = "default-agent";
    private String endpoint = "http://localhost:8080/api/metrics";
    private long intervalMs = 5000;
    // Phase 3 additions: which collectors and senders to enable
    private String[] collectors = new String[]{"cpu", "memory"};
    private String[] senders = new String[]{"http"};
    // Simulated kafka sink file (for Kafka sender)
    private String kafkaFile = "kafka-sink.log";

    // getters (encapsulation)
    public String getAgentId() { return agentId; }
    public String getEndpoint() { return endpoint; }
    public long getIntervalMs() { return intervalMs; }

    public String[] getCollectors() { return collectors; }
    public String[] getSenders() { return senders; }
    public String getKafkaFile() { return kafkaFile; }

    // load from JSON file (returns defaults on failure)
    public static AgentConfig loadFromFile(File f) {
        try (FileReader fr = new FileReader(f)) {
            return new Gson().fromJson(fr, AgentConfig.class);
        } catch (Exception e) {
            System.err.println("AgentConfig.loadFromFile failed, using defaults: " + e.getMessage());
            return new AgentConfig();
        }
    }

    @Override
    public String toString() {
        return "AgentConfig{" +
                "agentId='" + agentId + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", intervalMs=" + intervalMs +
                '}';
    }
}
