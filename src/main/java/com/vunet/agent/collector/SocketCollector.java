package com.vunet.agent.collector;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.vunet.agent.Metric;
import com.vunet.agent.batch.BufferedBatcher;

/**
 * SocketCollector: runs its own server thread. When a client connects and sends lines of
 * "metric.name value", it converts each to Metric and pushes to the provided batcher.
 */
public class SocketCollector implements Runnable {
    private final int port;
    private volatile boolean running = true;
    private final BufferedBatcher batcher;
    private Thread serverThread;

    public SocketCollector(int port, BufferedBatcher batcher) {
        this.port = port;
        this.batcher = batcher;
    }

    public void start() {
        serverThread = new Thread(this, "socket-collector-" + port);
        serverThread.setDaemon(true);
        serverThread.start();
        System.out.println("[SocketCollector] listening on port " + port);
    }

    public void stop() {
        running = false;
        try { serverThread.interrupt(); } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try (ServerSocket ss = new ServerSocket(port)) {
            while (running) {
                Socket s = ss.accept();
                // handle in separate thread so server can accept more
                new Thread(() -> handleClient(s)).start();
            }
        } catch (Exception e) {
            if (running) e.printStackTrace();
        }
    }

    private void handleClient(Socket s) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                // parse: "metric.name value"
                String[] parts = line.trim().split("\\s+");
                if (parts.length >= 2) {
                    try {
                        String name = parts[0];
                        double value = Double.parseDouble(parts[1]);
                        Metric m = new Metric(name, value, System.currentTimeMillis());
                        boolean ok = batcher.add(m);
                        if (!ok) System.err.println("[SocketCollector] buffer full, dropped metric: " + m);
                    } catch (NumberFormatException nfe) {
                        System.err.println("[SocketCollector] invalid number: " + line);
                    }
                }
            }
        } catch (Exception e) {
            // client closed or error
        } finally {
            try { s.close(); } catch (Exception ignored) {}
        }
    }
}