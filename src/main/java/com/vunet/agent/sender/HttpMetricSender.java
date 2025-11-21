package com.vunet.agent.sender;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.vunet.agent.Metric;

/**
 * Simple HTTP sender that POSTs a JSON array of metrics to an endpoint.
 */
public class HttpMetricSender implements MetricSender {

    private final String endpoint;

    public HttpMetricSender(String endpoint) {
        this.endpoint = endpoint;
    }

    @Override
    public void send(List<Metric> metrics) throws IOException {
        StringBuilder json = new StringBuilder();
        json.append('[');
        for (int i = 0; i < metrics.size(); i++) {
            json.append(metrics.get(i).toJson());
            if (i < metrics.size() - 1) json.append(',');
        }
        json.append(']');

        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

        byte[] payload = json.toString().getBytes(StandardCharsets.UTF_8);
        conn.setFixedLengthStreamingMode(payload.length);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload);
            os.flush();
        }

        int code = conn.getResponseCode();
        conn.disconnect();
        System.out.println("[Sender] POST " + endpoint + " -> " + code);
    }
}
