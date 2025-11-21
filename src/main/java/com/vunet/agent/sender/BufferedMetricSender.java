package com.vunet.agent.sender;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.vunet.agent.Metric;
import com.vunet.agent.MetricBatch;

/**
 * Buffered sender that collects incoming metrics into batches and forwards them
 * to an underlying MetricSender. Failed sends are retried with exponential backoff.
 */
public class BufferedMetricSender implements MetricSender {

    private final MetricSender delegate;
    private final BlockingQueue<Metric> queue;
    private final int batchSize;
    private final long batchTimeoutMs;
    private final int maxRetries;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService senderExecutor;
    private final Thread worker;
    private volatile boolean running = true;

    public BufferedMetricSender(MetricSender delegate, int batchSize, long batchTimeoutMs, int maxRetries) {
        this.delegate = delegate;
        this.batchSize = Math.max(1, batchSize);
        this.batchTimeoutMs = Math.max(0, batchTimeoutMs);
        this.maxRetries = Math.max(0, maxRetries);
        this.queue = new LinkedBlockingQueue<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.senderExecutor = Executors.newFixedThreadPool(2);

        this.worker = new Thread(this::runLoop, "buffered-sender-worker");
        this.worker.setDaemon(true);
        this.worker.start();
    }

    @Override
    public void send(List<Metric> metrics) throws IOException {
        if (!running) throw new IOException("BufferedMetricSender is shut down");
        for (Metric m : metrics) {
            try {
                queue.put(m);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted while enqueuing metric", e);
            }
        }
    }

    private void runLoop() {
        try {
            while (running) {
                List<Metric> batch = new ArrayList<>(batchSize);
                // block until first element available
                Metric first = queue.poll(batchTimeoutMs > 0 ? batchTimeoutMs : Long.MAX_VALUE, TimeUnit.MILLISECONDS);
                if (first != null) batch.add(first);
                else {
                    // timed out waiting
                    continue;
                }

                queue.drainTo(batch, batchSize - 1); // we already have one

                if (!batch.isEmpty()) {
                    MetricBatch mb = new MetricBatch(batch);
                    submitWithRetry(mb, 0, 0L);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            shutdownExecutors();
        }
    }

    private void submitWithRetry(MetricBatch mb, int attempt, long delayMs) {
        Runnable task = () -> {
            senderExecutor.submit(() -> {
                try {
                    delegate.send(mb.getMetrics());
                } catch (IOException e) {
                    int nextAttempt = attempt + 1;
                    if (nextAttempt <= maxRetries) {
                        long backoff = calculateBackoff(nextAttempt);
                        System.err.println("[BufferedSender] send failed, scheduling retry " + nextAttempt + " in " + backoff + "ms: " + e.getMessage());
                        submitWithRetry(mb, nextAttempt, backoff);
                    } else {
                        System.err.println("[BufferedSender] send failed after " + attempt + " attempts: " + e.getMessage());
                    }
                }
            });
        };

        if (delayMs > 0) {
            scheduler.schedule(task, delayMs, TimeUnit.MILLISECONDS);
        } else {
            task.run();
        }
    }

    private long calculateBackoff(int attempt) {
        // simple exponential backoff with jitter
        long base = 200L; // ms
        long backoff = base * (1L << (attempt - 1));
        long jitter = ThreadLocalRandom.current().nextLong(0, base);
        return Math.min(backoff + jitter, Duration.ofSeconds(30).toMillis());
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
        shutdownExecutors();
    }

    private void shutdownExecutors() {
        try {
            senderExecutor.shutdown();
            senderExecutor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        try {
            scheduler.shutdown();
            scheduler.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}
