package com.vunet.agent.sender;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.vunet.agent.Metric;

class AsyncHttpMetricSenderTest {

    static HttpServer server;
    static int port = 18080;
    static volatile String lastBody;

    @BeforeAll
    static void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/metrics", (HttpExchange ex) -> {
            InputStream is = ex.getRequestBody();
            byte[] b = is.readAllBytes();
            lastBody = new String(b);
            ex.sendResponseHeaders(200, 0);
            ex.getResponseBody().close();
        });
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop(0);
    }

    @Test
    void senderPostsBodyAndReturns() throws InterruptedException {
        AsyncHttpMetricSender sender = new AsyncHttpMetricSender("http://localhost:" + port + "/metrics", 2, 100);
        Metric m = new Metric("t", 1.0, System.currentTimeMillis());
        sender.send(List.of(m));

        // wait a bit for async send to complete
        Thread.sleep(500);
        assertTrue(lastBody != null && lastBody.contains("\"name\":\"t\""));
        sender.shutdown();
    }
}
