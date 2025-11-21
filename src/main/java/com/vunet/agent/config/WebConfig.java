package com.vunet.agent.config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;

/**
 * Plain Java configuration used by SpringTomcatConfig.
 * Immutable, safe, and simple.
 */
public final class WebConfig {

    public static final String ENV_PORT = "VUAGENT_PORT";
    public static final String SYS_PROP_PORT = "agent.port";
    private static final int DEFAULT_PORT = 9001;

    private final int port;
    private final InetAddress address;

    public WebConfig() {
        this(readPortFromEnvOrProperty().orElse(DEFAULT_PORT), getDefaultLoopback());
    }

    public WebConfig(int port, InetAddress address) {
        validatePort(port);
        this.port = port;
        this.address = address != null ? address : getDefaultLoopback();
    }

    private static Optional<Integer> readPortFromEnvOrProperty() {
        String prop = System.getProperty(SYS_PROP_PORT);
        if (prop != null && !prop.isBlank()) {
            try {
                return Optional.of(Integer.parseInt(prop.trim()));
            } catch (NumberFormatException ignored) {}
        }

        String env = System.getenv(ENV_PORT);
        if (env != null && !env.isBlank()) {
            try {
                return Optional.of(Integer.parseInt(env.trim()));
            } catch (NumberFormatException ignored) {}
        }

        return Optional.empty();
    }

    private static InetAddress getDefaultLoopback() {
        try {
            return InetAddress.getLoopbackAddress();
        } catch (Throwable t) {
            try {
                return InetAddress.getByName("127.0.0.1");
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to determine loopback address", e);
            }
        }
    }

    private static void validatePort(int port) {
        if (port <= 0 || port > 65535) {
            throw new IllegalArgumentException("Port out of range (1–65535): " + port);
        }
    }

    // getters used by SpringTomcatConfig
    public int getPort() {
        return port;
    }

    public InetAddress getAddress() {
        return address;
    }

    @Override
    public String toString() {
        return "WebConfig{port=" + port + ", address=" + address.getHostAddress() + "}";
    }
}