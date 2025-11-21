package com.vunet.agent.batch;

import java.util.ArrayList;
import java.util.List;

import com.vunet.agent.Metric;

public class BatchBuilder {
    private final List<Metric> items = new ArrayList<>();

    public BatchBuilder add(Metric m) {
        items.add(m);
        return this;
    }

    public BatchBuilder addAll(List<Metric> ms) {
        items.addAll(ms);
        return this;
    }

    public MetricBatch build() {
        return new MetricBatch(new ArrayList<>(items));
    }

    public int size() { return items.size(); }

    public void clear() { items.clear(); }
}
