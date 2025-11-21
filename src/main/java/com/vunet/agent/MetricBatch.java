package com.vunet.agent;

import java.util.List;
import java.util.Objects;

/**
 * Immutable batch of metrics. Provides a small helper to serialize to JSON array.
 */
public final class MetricBatch {
    private final List<Metric> metrics;

    public MetricBatch(List<Metric> metrics) {
        this.metrics = List.copyOf(Objects.requireNonNull(metrics));
    }

    public List<Metric> getMetrics() {
        return metrics;
    }

    public int size() { return metrics.size(); }

    public String toJsonArray() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for (int i = 0; i < metrics.size(); i++) {
            sb.append(metrics.get(i).toJson());
            if (i < metrics.size() - 1) sb.append(',');
        }
        sb.append(']');
        return sb.toString();
    }
}
