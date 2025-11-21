package com.vunet.agent.sender;

import java.io.IOException;
import java.util.List;

import com.vunet.agent.Metric;

/**
 * Strategy interface for sending metrics to a backend.
 */
public interface MetricSender {
    void send(List<Metric> metrics) throws IOException;
}
