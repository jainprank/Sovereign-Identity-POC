package com.idtrust.app.config;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Registers {@link TenantConnectionProvider} as Hibernate's JDBC connection provider so every
 * connection Hibernate uses has the {@code app.tenant_id} Postgres session variable set for Row
 * Level Security (see {@link TenantConnectionProvider} and the {@code V3} migration).
 */
@Configuration
public class HibernateTenantConfig {

    @Bean
    public HibernatePropertiesCustomizer tenantConnectionProviderCustomizer(DataSource dataSource) {
        return properties -> {
            properties.put("hibernate.connection.datasource", dataSource);
            properties.put(AvailableSettings.CONNECTION_PROVIDER, TenantConnectionProvider.class.getName());
        };
    }
}
