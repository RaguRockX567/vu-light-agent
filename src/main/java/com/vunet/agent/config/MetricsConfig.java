package com.vunet.agent.config;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.MeterBinder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.lang.management.ManagementFactory;
import com.sun.management.OperatingSystemMXBean;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterBinder processMemoryMetrics() {
        return registry -> {
            OperatingSystemMXBean osBean = (com.sun.management.OperatingSystemMXBean) 
                ManagementFactory.getOperatingSystemMXBean();
                
            Gauge.builder("system.cpu.usage", osBean, os -> osBean.getCpuLoad() * 100)
                .description("System CPU Usage")
                .baseUnit("percent")
                .register(registry);
                
            Gauge.builder("system.memory.used", osBean, 
                os -> {
                    long total = ((com.sun.management.OperatingSystemMXBean) os).getTotalMemorySize();
                    long free = ((com.sun.management.OperatingSystemMXBean) os).getFreeMemorySize();
                    return (double)(total - free) / (1024 * 1024 * 1024);
                })
                .description("Used Memory in GB")
                .baseUnit("GB")
                .register(registry);
                
            Gauge.builder("system.memory.total", osBean, 
                os -> (double)((com.sun.management.OperatingSystemMXBean) os).getTotalMemorySize() / (1024 * 1024 * 1024))
                .description("Total Memory in GB")
                .baseUnit("GB")
                .register(registry);
        };
    }
    
    @Bean
    public MeterBinder customJvmMemoryMetrics() {
        return registry -> {
            Runtime runtime = Runtime.getRuntime();
            
            Gauge.builder("jvm.memory.used", runtime, 
                r -> (double)(r.totalMemory() - r.freeMemory()) / (1024 * 1024))
                .description("JVM Used Memory")
                .baseUnit("MB")
                .register(registry);
                
            Gauge.builder("jvm.memory.max", runtime, 
                r -> (double)r.maxMemory() / (1024 * 1024))
                .description("JVM Max Memory")
                .baseUnit("MB")
                .register(registry);
        };
    }
}
