package com.vunet.agent;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication(scanBasePackages = "com.vunet.agent")
public class AgentApplication {
    public static void main(String[] args) {
        System.out.println("🚀 VuNet LightAgent — Spring REST API starting...");
        SpringApplication.run(AgentApplication.class, args);
    }

    @Bean
    CommandLineRunner startAgentOnBoot(@Value("${server.port:9001}") int port) {
        return args -> {
            // Start background agent thread
            AgentMain.startAgent();
            System.out.println("VuNet Light Agent started on port " + port);
            System.out.println("Listening for metrics...");
        };
    }
}