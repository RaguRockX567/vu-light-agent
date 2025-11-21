package com.vunet.agent.batch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.vunet.agent.Metric;
import com.vunet.agent.metrics.AgentMetrics;
import com.vunet.agent.sender.MetricSender;

/**
 * BufferedBatcher collects individual metrics and emits Metric batches to sender.
 * - bounded queue with drop-oldest policy
 * - flushes on batch size or scheduled interval
 * - uses a small sender executor to perform sends asynchronously
 * - updates AgentMetrics counters
 */
public class BufferedBatcher {
    private final BlockingQueue<Metric> queue;
    private final int batchSize;
    private final long flushIntervalMs;
    private final MetricSender sender;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService senderExec;
    private final AgentMetrics metrics = AgentMetrics.getInstance();

    public BufferedBatcher(int capacity, int batchSize, long flushIntervalMs, MetricSender sender) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.batchSize = batchSize;
        this.flushIntervalMs = flushIntervalMs;
        this.sender = sender;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "batch-flusher");
            t.setDaemon(true);
            return t;
        });

        this.senderExec = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "sender-worker");
            t.setDaemon(true);
            return t;
        });

        // schedule periodic flush
        scheduler.scheduleAtFixedRate(this::flushIfAny, this.flushIntervalMs, this.flushIntervalMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Add metric to buffer. If queue full, drop oldest and insert.
     */
    public boolean add(Metric m) {
        metrics.incrementCollected();
        boolean ok = queue.offer(m);
        if (!ok) {
            Metric dropped = queue.poll(); // drop oldest
            if (dropped != null) metrics.incrementDropped();
            ok = queue.offer(m);
            if (!ok) {
                // if still cannot offer (very rare), count drop
                metrics.incrementDropped();
                return false;
            }
        }
        if (queue.size() >= batchSize) {
            // schedule immediate flush in sender executor
            senderExec.submit(this::flushIfAny);
        }
        return ok;
    }

    private void flushIfAny() {
        try {
            List<Metric> batch = new ArrayList<>(batchSize);
            queue.drainTo(batch, batchSize);
            if (batch.isEmpty()) return;

            // send asynchronously so flush thread returns quickly
            senderExec.submit(() -> {
                try {
                    sender.send(batch);
                    metrics.incrementSent(batch.size());
                } catch (Exception e) {
                    System.err.println("[BufferedBatcher] send failure: " + e.getMessage());
                    metrics.incrementDropped(batch.size());
                }
            });
        } catch (Exception e) {
            System.err.println("[BufferedBatcher] flush failed: " + e.getMessage());
        }
    }

    public int queuedSize() {
        return queue.size();
    }

    /**
     * Returns true if queue is considered above the given ratio threshold.
     * Example: ratio=0.8 checks if queue size >= capacity * 0.8
     */
    public boolean isAboveThreshold(double ratio) {
        int capacity = queue.remainingCapacity() + queue.size();
        return queue.size() >= (int) (capacity * ratio);
    }

    public boolean isFull() {
        return queue.remainingCapacity() == 0;
    }

    public void shutdown() {
        try {
            scheduler.shutdownNow();
            flushIfAny();
            senderExec.shutdown();
            senderExec.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
