package com.idtrust.apiplatform.log;

import java.time.Instant;
import java.util.UUID;

public final class ApiRequestLogDtos {

    private ApiRequestLogDtos() {
    }

    public record ApiRequestLogResponse(
            UUID id,
            UUID apiClientId,
            String method,
            String path,
            int statusCode,
            long durationMs,
            String correlationId,
            Instant createdAt
    ) {
        public static ApiRequestLogResponse from(ApiRequestLog log) {
            return new ApiRequestLogResponse(log.getId(), log.getApiClientId(), log.getMethod(), log.getPath(),
                    log.getStatusCode(), log.getDurationMs(), log.getCorrelationId(), log.getCreatedAt());
        }
    }
}
