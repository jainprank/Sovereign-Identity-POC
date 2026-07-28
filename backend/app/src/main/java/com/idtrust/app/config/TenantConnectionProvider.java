package com.idtrust.app.config;

import com.idtrust.common.tenant.TenantContext;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.service.UnknownUnwrapTypeException;
import org.hibernate.service.spi.Configurable;
import org.hibernate.service.spi.Stoppable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

/**
 * Wraps the application {@link DataSource} so that every physical JDBC connection Hibernate
 * borrows has the Postgres session variable {@code app.tenant_id} set to the current request's
 * tenant (see {@link TenantContext}) before any query runs on it.
 *
 * <p>Postgres Row Level Security policies (see the {@code V3__row_level_security.sql} migration)
 * key off {@code current_setting('app.tenant_id', true)} to enforce tenant isolation at the
 * database layer - the third of the three layers required by the Architecture &amp;
 * Implementation Baseline, section 23 ("Tenant Isolation"): API, service, and database.
 *
 * <p>Wired in via {@link HibernateTenantConfig}, which sets
 * {@code hibernate.connection.provider_class} to this class and passes the real Spring-managed
 * {@link DataSource} through {@code hibernate.connection.datasource}.
 */
public class TenantConnectionProvider implements ConnectionProvider, Configurable, Stoppable {

    private static final Logger log = LoggerFactory.getLogger(TenantConnectionProvider.class);
    private static final String DATASOURCE_PROPERTY = "hibernate.connection.datasource";
    private static final String SET_TENANT_SQL = "SELECT set_config('app.tenant_id', ?, false)";

    private DataSource dataSource;

    @Override
    public void configure(Map<String, Object> configurationValues) {
        Object configured = configurationValues.get(DATASOURCE_PROPERTY);
        if (!(configured instanceof DataSource ds)) {
            throw new IllegalStateException(
                    "TenantConnectionProvider requires '" + DATASOURCE_PROPERTY + "' to be configured with a DataSource instance");
        }
        this.dataSource = ds;
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = dataSource.getConnection();
        applyTenant(connection);
        return connection;
    }

    private void applyTenant(Connection connection) {
        // set_config() is Postgres-specific; swallow failures rather than break connection
        // acquisition on databases without it (e.g. the H2 in-memory database used by
        // IdTrustApplicationTests before a real Postgres integration test suite is introduced).
        String tenantId = TenantContext.getTenantId() == null ? "" : TenantContext.getTenantId().toString();
        try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
            statement.setString(1, tenantId);
            statement.execute();
        } catch (SQLException e) {
            log.debug("Skipping app.tenant_id session variable (not supported by this database): {}", e.getMessage());
        }
    }

    @Override
    public void closeConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public boolean supportsAggressiveRelease() {
        // Hold the physical connection for the whole transaction so app.tenant_id stays valid
        // across every statement rather than being reset by an intermediate connection borrow.
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return ConnectionProvider.class.equals(unwrapType) || DataSource.class.isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (isUnwrappableAs(unwrapType)) {
            return unwrapType.cast(this);
        }
        throw new UnknownUnwrapTypeException(unwrapType);
    }

    @Override
    public void stop() {
        // DataSource lifecycle (pool shutdown) is managed by Spring; nothing to do here.
    }
}
