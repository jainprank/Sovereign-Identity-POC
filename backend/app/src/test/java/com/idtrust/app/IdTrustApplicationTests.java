package com.idtrust.app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Verifies the application context (all modules) wires up correctly. Uses an in-memory-friendly
 * datasource override so this test doesn't require a running Postgres instance; integration
 * tests against a real Postgres (via Testcontainers) are introduced starting E02/E03.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:idtrust;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false"
})
class IdTrustApplicationTests {

    @Test
    void contextLoads() {
    }
}
