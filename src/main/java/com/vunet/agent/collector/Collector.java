package com.vunet.agent.collector;

import java.util.List;

import com.vunet.agent.Metric;

/**
 * Collector interface for producing metrics.
 */
public interface Collector {
    String getName();
    List<Metric> collect();
}
