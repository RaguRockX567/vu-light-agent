package com.vunet.agent;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vunet.agent.batch.BufferedBatcher;
import com.vunet.agent.collector.Collector;
import com.vunet.agent.collector.CollectorFactory;
import com.vunet.agent.collector.SocketCollector;
import com.vunet.agent.metrics.AgentMetrics;
import com.vunet.agent.sender.AsyncHttpMetricSender;
import com.vunet.agent.sender.MetricSender;

/**
 * AgentMain: contains a startAgent() method that launches the agent
 * work on a new daemon thread and returns immediately.
 */
public class AgentMain {

    private static Thread agentThread;
    private static volatile BufferedBatcher batcher;
    private static volatile SocketCollector socketCollector;
    private static volatile ScheduledExecutorService scheduler;
    private static volatile MetricSender httpSender;

    // startAgent is idempotent: calling it when agent already started has no effect.
    public static synchronized void startAgent() {
        if (agentThread != null && agentThread.isAlive()) {
            System.out.println("[AgentMain] Agent already running.");
            return;
        }

        agentThread = new Thread(() -> {
            try {
                runAgent();
            } catch (Throwable t) {
                System.err.println("[AgentMain] Uncaught error in agent thread: " + t.getMessage());
                t.printStackTrace();
            }
        }, "vu-agent-main");
        agentThread.setDaemon(true);
        agentThread.start();
        System.out.println("[AgentMain] Agent background thread started.");
    }

    // Stop method for graceful shutdown
    public static synchronized void stopAgent() {
        if (agentThread != null) {
            agentThread.interrupt();
            try {
                agentThread.join(2000);
            } catch (InterruptedException ignored) {
            }
            agentThread = null;
            System.out.println("[AgentMain] Agent stopped.");

            // Clean up resources
            if (scheduler != null) {
                scheduler.shutdownNow();
                scheduler = null;
            }
            if (socketCollector != null) {
                socketCollector.stop();
                socketCollector = null;
            }
            if (batcher != null) {
                batcher.shutdown();
                batcher = null;
            }
            if (httpSender instanceof AsyncHttpMetricSender s) {
                s.shutdown();
            }
            httpSender = null;
        }
    }

    // The core agent logic, refactored to a method
    private static void runAgent() {
        System.out.println("VuNet LightAgent — Phase 4 starting...");

        // Load config from file if present
        File cfgFile = new File("agent-config.json");
        AgentConfig cfg = cfgFile.exists() ? AgentConfig.loadFromFile(cfgFile) : new AgentConfig();

        // Environment overrides
        String envEndpoint = System.getenv("BACKEND_URL");
        String endpoint = (envEndpoint != null && !envEndpoint.isBlank()) ? envEndpoint : cfg.getEndpoint();
        String envInterval = System.getenv("AGENT_INTERVAL_MS");
        long intervalMs = cfg.getIntervalMs();
        if (envInterval != null && !envInterval.isBlank()) {
            try { intervalMs = Long.parseLong(envInterval.trim()); } catch (NumberFormatException ignored) {}
        }
        System.out.println("Config: endpoint=" + endpoint + ", intervalMs=" + intervalMs);

        AgentMetrics metrics = AgentMetrics.getInstance();
        metrics.registerMBean();

        // prepare sender + batcher
        httpSender = new AsyncHttpMetricSender(endpoint, 3, 200L);
        batcher = new BufferedBatcher(1000, 10, Math.max(500, intervalMs), httpSender);

        // schedule collectors
        List<Collector> collectors = CollectorFactory.createCollectors(cfg.getCollectors());
        scheduler = Executors.newScheduledThreadPool(Math.max(1, collectors.size()));

        for (var c : collectors) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    // simple backpressure: skip when very full
                    if (batcher.queuedSize() > 800) {
                        System.err.println("[AgentMain] backpressure: skipping collection from " + c.getName());
                        metrics.incrementDropped();
                        return;
                    }
                    var list = c.collect();
                    list.forEach(batcher::add);
                } catch (Exception e) {
                    System.err.println("[AgentMain] collector error: " + e.getMessage());
                }
            }, 0, intervalMs, TimeUnit.MILLISECONDS);
        }

        // socket collector (non-blocking thread)
        socketCollector = new SocketCollector(4711, batcher);
        socketCollector.start();

        // keep running until interrupted
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Thread.sleep(1000L);
            }
        } catch (InterruptedException ignored) {
            // fall through to shutdown
        }
    }

    // Helper method for metrics endpoint
    public static int getCurrentQueueSize() {
        return batcher != null ? batcher.queuedSize() : 0;
    }
}
