package com.vunet.agent;

import java.time.Instant;
import java.util.Objects;

/**
 * Immutable Metric value object (no external libs).
 */
public final class Metric {
    private final String name;
    private final double value;
    private final long timestamp;

    public Metric(String name, double value, long timestamp) {
        this.name = Objects.requireNonNull(name, "name");
        this.value = value;
        this.timestamp = timestamp;
    }

    public String getName() { return name; }
    public double getValue() { return value; }
    public long getTimestamp() { return timestamp; }

    // Simple JSON builder — sufficient for demo and Phase 1
    public String toJson() {
        // Note: this is simple and doesn't escape quotes in name; good enough for demo
        return String.format("{\"name\":\"%s\",\"value\":%s,\"timestamp\":%d}",
                name, Double.toString(value), timestamp);
    }

    @Override
    public String toString() {
        return "Metric{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", timestamp=" + Instant.ofEpochMilli(timestamp).toString() +
                '}';
    }
}
