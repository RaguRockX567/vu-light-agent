package com.vunet.agent.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Spring Boot configuration that customizes Tomcat settings.
 * This class is only loaded when running with Spring Boot.
 */
@Configuration
public class SpringTomcatConfig {


    @Value("${server.port:9001}")
    private int serverPort;

    @Value("${server.address:0.0.0.0}")
    private String serverAddress;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> servletContainer() {
        return factory -> {
            factory.setPort(serverPort);
            try {
                factory.setAddress(InetAddress.getByName(serverAddress));
            } catch (UnknownHostException ignored) {
                // leave default binding if resolution fails
            }
            factory.addConnectorCustomizers(connector -> connector.setProperty("bindOnInit", "true"));
        };
    }
}
