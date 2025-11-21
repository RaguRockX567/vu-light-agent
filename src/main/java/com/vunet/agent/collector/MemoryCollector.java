package com.vunet.agent.collector;

import java.util.Collections;
import java.util.List;

import com.vunet.agent.Metric;

/**
 * Collects simple JVM memory usage metric.
 */
public class MemoryCollector implements Collector {

    @Override
    public String getName() { return "memory"; }

    @Override
    public List<Metric> collect() {
        Runtime rt = Runtime.getRuntime();
        long total = rt.totalMemory();
        long free = rt.freeMemory();
        double usage = total > 0 ? (double) (total - free) / (double) total : 0.0;

        Metric m = new Metric("system.memory.usage", usage, System.currentTimeMillis());
        return Collections.singletonList(m);
    }
}
