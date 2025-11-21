package com.vunet.agent.batch;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.vunet.agent.Metric;

public final class MetricBatch {
    private final List<Metric> metrics;

    public MetricBatch(List<Metric> metrics) {
        this.metrics = Collections.unmodifiableList(Objects.requireNonNull(metrics));
    }

    public List<Metric> getMetrics() { return metrics; }

    public int size() { return metrics.size(); }

    @Override
    public String toString() {
        return "MetricBatch{size=" + metrics.size() + "}";
    }
}
