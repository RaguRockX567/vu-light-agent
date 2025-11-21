package com.vunet.agent.collector;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple factory to create collectors by name.
 */
public final class CollectorFactory {

    private CollectorFactory() {}

    public static List<Collector> createCollectors(String[] names) {
        if (names == null || names.length == 0) {
            // default: both cpu and memory
            return List.of(create("cpu"), create("memory"));
        }
        List<Collector> collectors = new ArrayList<>();
        for (String name : names) {
            Collector c = create(name);
            if (c != null) collectors.add(c);
        }
        return collectors;
    }

    public static Collector create(String name) {
        if (name == null) return null;
        switch (name.trim().toLowerCase()) {
            case "cpu":
                return new CpuCollector();
            case "memory":
                return new MemoryCollector();
            default:
                return null;
        }
    }
}
