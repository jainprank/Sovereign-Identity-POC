package com.idtrust.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Composition root for the IDTrust modular monolith.
 *
 * <p>Per the Architecture &amp; Implementation Baseline (Decision 1), IDTrust starts as a single
 * deployable application with clear domain module boundaries ({@code identity}, {@code did},
 * {@code kms}, {@code credential}, {@code wallet}, {@code verification}, {@code trust},
 * {@code hedera}), rather than 20+ independent microservices. Services are extracted later only
 * where scale or organizational boundaries justify it.
 *
 * <p>Component/entity/repository/{@code @ConfigurationProperties} scanning is rooted at
 * {@code com.idtrust} so every module is picked up automatically without this class needing to
 * change as modules are implemented.
 */
@SpringBootApplication(scanBasePackages = "com.idtrust")
@EntityScan(basePackages = "com.idtrust")
@EnableJpaRepositories(basePackages = "com.idtrust")
@ConfigurationPropertiesScan(basePackages = "com.idtrust")
public class IdTrustApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdTrustApplication.class, args);
    }
}
