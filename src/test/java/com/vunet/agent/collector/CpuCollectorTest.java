package com.vunet.agent.collector;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.vunet.agent.Metric;

class CpuCollectorTest {
    @Test
    void collectReturnsMetric() {
        CpuCollector c = new CpuCollector();
        List<Metric> m = c.collect();
        assertNotNull(m);
        assertFalse(m.isEmpty());
        Metric first = m.get(0);
        assertEquals("system.cpu.load", first.getName());
        assertTrue(first.getValue() >= 0.0);
    }
}
