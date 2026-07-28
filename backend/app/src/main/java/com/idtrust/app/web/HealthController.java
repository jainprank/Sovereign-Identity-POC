package com.idtrust.app.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Public, unauthenticated health endpoint matching the {@code Health} tag in the IDTrust API
 * specification. Complements (does not replace) Spring Boot Actuator's {@code /actuator/health},
 * which is used for Kubernetes liveness/readiness probes.
 */
@RestController
public class HealthController {

    @GetMapping("/api/v1/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "idtrust",
                "timestamp", Instant.now().toString()
        );
    }
}
