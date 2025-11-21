package com.vunet.agent.health;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.vunet.agent.batch.BufferedBatcher;
import com.vunet.agent.metrics.AgentMetrics;

/**
 * Minimal health server exposing /health and /metrics (agent internal stats) as JSON.
 */
public class HealthServer {
    private final HttpServer server;
    private final BufferedBatcher batcher;
    private final AgentMetrics metrics = AgentMetrics.getInstance();

    public HealthServer(int port, BufferedBatcher batcher) throws Exception {
        this.batcher = batcher;
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", new HealthHandler());
        server.createContext("/metrics", new MetricsHandler());
        server.setExecutor(null);
    }

    public void start() {
        metrics.registerMBean();
        server.start();
        System.out.println("[HealthServer] started on port " + server.getAddress().getPort());
    }

    public void stop() {
        server.stop(0);
    }

    class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                String resp = "{\"status\":\"UP\"}";
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(resp.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    class MetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                Map<String, Object> m = new HashMap<>();
                m.put("uptimeMs", metrics.getUptimeMs());
                m.put("collected", metrics.getCollectedCount());
                m.put("sent", metrics.getSentCount());
                m.put("dropped", metrics.getDroppedCount());
                m.put("queueSize", batcher.queuedSize());
                String json = toJson(m);
                exchange.getResponseHeaders().add("Content-Type","application/json");
                exchange.sendResponseHeaders(200, json.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String toJson(Map<String,Object> map){
            StringBuilder sb = new StringBuilder("{");
            boolean first=true;
            for (var e : map.entrySet()) {
                if(!first) sb.append(",");
                first=false;
                sb.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            }
            sb.append("}");
            return sb.toString();
        }
    }
}