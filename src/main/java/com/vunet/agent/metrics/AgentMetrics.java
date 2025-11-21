package com.vunet.agent.metrics;

import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicLong;
import javax.management.*;

/**
 * Singleton holding basic counters and registering as an MBean.
 */
public class AgentMetrics implements AgentMetricsMBean {
    private static final AgentMetrics INSTANCE = new AgentMetrics();

    private final AtomicLong collected = new AtomicLong(0);
    private final AtomicLong sent = new AtomicLong(0);
    private final AtomicLong dropped = new AtomicLong(0);
    private final long startTime = System.currentTimeMillis();
    private ObjectName objectName;

    private AgentMetrics() { /* private */ }

    public static AgentMetrics getInstance() { return INSTANCE; }

    public void registerMBean() {
        try {
            MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
            objectName = new ObjectName("com.vunet.agent.metrics:type=AgentMetrics");
            // unregister if already registered
            if (mbs.isRegistered(objectName)) mbs.unregisterMBean(objectName);
            mbs.registerMBean(this, objectName);
        } catch (Exception e) {
            System.err.println("Failed to register AgentMetrics MBean: " + e.getMessage());
        }
    }

    public void incrementCollected() { collected.incrementAndGet(); }
    public void incrementSent(int n) { sent.addAndGet(n); }
    public void incrementDropped() { dropped.incrementAndGet(); }
    public void incrementDropped(int n) { dropped.addAndGet(n); }

    // MBean getters
    @Override public long getUptimeMs() { return System.currentTimeMillis() - startTime; }
    @Override public long getCollectedCount() { return collected.get(); }
    @Override public long getSentCount() { return sent.get(); }
    @Override public long getDroppedCount() { return dropped.get(); }
    @Override public long getStartTimeMs() { return startTime; }
}