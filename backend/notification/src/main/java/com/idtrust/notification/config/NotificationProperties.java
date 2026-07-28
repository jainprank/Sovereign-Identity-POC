package com.idtrust.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * FR-NOT-001 (Email Notification) configuration. Mirrors {@code hedera.config.HederaProperties}'
 * "real implementation, disabled by default, enabled by explicitly supplying real credentials"
 * precedent: {@link #enabled()} defaults to {@code false} so this platform is fully operational
 * without a real SMTP account, and {@link com.idtrust.notification.email.EmailNotificationService}
 * treats {@code disabled} as a valid, healthy operating mode (log-and-skip), never an error.
 */
@Configuration
@ConfigurationProperties(prefix = "idtrust.notification.email")
public class NotificationProperties {

    private boolean enabled = false;
    private String host;
    private int port = 587;
    private String username;
    private String password;
    private String from = "no-reply@idtrust.example";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }
}
