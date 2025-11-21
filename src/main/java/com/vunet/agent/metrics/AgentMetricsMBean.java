package com.vunet.agent.metrics;

/**
 * JMX MBean interface for agent metrics.
 */
public interface AgentMetricsMBean {
    long getUptimeMs();
    long getCollectedCount();
    long getSentCount();
    long getDroppedCount();
    long getStartTimeMs();
}