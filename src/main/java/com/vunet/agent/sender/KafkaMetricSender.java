package com.vunet.agent.sender;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import com.vunet.agent.Metric;

/**
 * Simulated Kafka sender: writes metrics to a file (append) to simulate sending to Kafka.
 */
public class KafkaMetricSender implements MetricSender {

    private final String sinkFile;

    public KafkaMetricSender(String sinkFile) {
        this.sinkFile = sinkFile;
    }

    @Override
    public void send(List<Metric> metrics) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(sinkFile, true))) {
            for (Metric m : metrics) {
                pw.println(m.toJson());
            }
            pw.flush();
        }
        System.out.println("[KafkaSim] Wrote " + metrics.size() + " metrics to " + sinkFile);
    }
}
