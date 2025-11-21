package com.vunet.agent.collector;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.List;

import com.sun.management.OperatingSystemMXBean;
import com.vunet.agent.Metric;

/**
 * CpuCollector: non-deprecated approach using com.sun.management.OperatingSystemMXBean.
 * getSystemCpuLoad returns 0.0-1.0 or negative if unavailable.
 */
public class CpuCollector implements Collector {
    private final OperatingSystemMXBean osBean;

    public CpuCollector() {
        this.osBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    }

    @Override
    public String getName() {
        return "cpu";
    }

    @Override
    public List<Metric> collect() {
        // Prefer non-deprecated API
        double cpu = osBean.getCpuLoad();
        if (cpu < 0) cpu = 0.0;
        Metric m = new Metric("system.cpu.load", cpu, System.currentTimeMillis());
        return Collections.singletonList(m);
    }
}
