package com.vunet.agent.sender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.vunet.agent.Metric;

/**
 * AsyncHttpMetricSender:
 * - builds a JSON array (from Metric.toJson())
 * - submits request via HttpClient asynchronously
 * - retries with exponential backoff on failure (CompletableFuture chaining)
 */
public class AsyncHttpMetricSender implements MetricSender {
    private final HttpClient client;
    private final URI endpoint;
    private final int maxRetries;
    private final long baseBackoffMs;
    private final ScheduledExecutorService retryScheduler = Executors.newScheduledThreadPool(1);

    public AsyncHttpMetricSender(String endpointUrl, int maxRetries, long baseBackoffMs) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.endpoint = URI.create(endpointUrl);
        this.maxRetries = maxRetries;
        this.baseBackoffMs = baseBackoffMs;
    }

    @Override
    public void send(List<Metric> metrics) {
        if (metrics == null || metrics.isEmpty()) return;
        String body = metrics.stream().map(Metric::toJson).collect(Collectors.joining(",", "[", "]"));
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(5))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        attemptSend(request, 0);
    }

    private void attemptSend(HttpRequest request, int attempt) {
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(resp -> {
                    int code = resp.statusCode();
                    if (code >= 200 && code < 300) {
                        System.out.println("[AsyncHttpSender] success: " + code + " bodyLen=" + resp.body().length());
                    } else {
                        System.err.println("[AsyncHttpSender] bad status: " + code);
                        scheduleRetry(request, attempt);
                    }
                })
                .exceptionally(ex -> {
                    System.err.println("[AsyncHttpSender] request failed: " + ex.getMessage());
                    scheduleRetry(request, attempt);
                    return null;
                });
    }

    private void scheduleRetry(HttpRequest request, int attempt) {
        if (attempt >= maxRetries) {
            System.err.println("[AsyncHttpSender] max retries reached, dropping batch");
            return;
        }
        long backoff = Math.min(baseBackoffMs * (1L << attempt), 10_000L);
        int nextAttempt = attempt + 1;
        retryScheduler.schedule(() -> attemptSend(request, nextAttempt), backoff, TimeUnit.MILLISECONDS);
        System.out.println("[AsyncHttpSender] scheduled retry #" + nextAttempt + " in " + backoff + "ms");
    }

    public void shutdown() {
        retryScheduler.shutdownNow();
    }
}
