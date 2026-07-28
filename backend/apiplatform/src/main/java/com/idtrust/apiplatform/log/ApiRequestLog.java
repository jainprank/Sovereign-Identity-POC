package com.idtrust.apiplatform.log;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A single completed API request (FR-API-007 API Logs), captured by
 * {@link ApiRequestLoggingFilter}. {@code tenantId}/{@code apiClientId} are both nullable: a
 * request that fails authentication, or hits a platform-level/permitted endpoint, never resolves
 * either. Mirrors {@code credential.status.CredentialStatusEvent}'s dedicated-history-table
 * precedent, rather than relying solely on {@code LoggingAuditEventPublisher}-style log lines.
 */
@Entity
@Table(name = "api_request_logs")
public class ApiRequestLog {

    @Id
    private UUID id;

    @Column(name = "tenant_id")
    private UUID tenantId;

    @Column(name = "api_client_id")
    private UUID apiClientId;

    @Column(nullable = false)
    private String method;

    @Column(nullable = false)
    private String path;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "duration_ms", nullable = false)
    private long durationMs;

    @Column(name = "correlation_id")
    private String correlationId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApiRequestLog() {
        // JPA
    }

    public ApiRequestLog(UUID tenantId, UUID apiClientId, String method, String path, int statusCode,
                          long durationMs, String correlationId) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.apiClientId = apiClientId;
        this.method = method;
        this.path = path;
        this.statusCode = statusCode;
        this.durationMs = durationMs;
        this.correlationId = correlationId;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getApiClientId() {
        return apiClientId;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
