package com.idtrust.hedera.health;

import com.hedera.hashgraph.sdk.AccountBalanceQuery;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.idtrust.hedera.config.HederaProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * FR-HED-013 (Hedera Health Check): exposed at {@code /actuator/health/hedera} alongside the
 * built-in db/diskSpace indicators. Reports {@code UP} (not {@code DOWN}) when anchoring is
 * disabled - a disabled Hedera integration is a valid, healthy operating mode for this platform
 * (see the {@code hedera} module's package-info.java), not an outage.
 */
@Component("hedera")
public class HederaHealthIndicator implements HealthIndicator {

    private final HederaProperties properties;

    public HederaHealthIndicator(HederaProperties properties) {
        this.properties = properties;
    }

    @Override
    public Health health() {
        if (!properties.enabled()) {
            return Health.up()
                    .withDetail("enabled", false)
                    .withDetail("reason", "Hedera anchoring is disabled (idtrust.hedera.enabled=false)")
                    .build();
        }
        Client client = null;
        try {
            client = Client.forName(properties.network());
            AccountId operator = AccountId.fromString(properties.accountId());
            client.setOperator(operator, PrivateKey.fromString(properties.operatorKey()));
            new AccountBalanceQuery().setAccountId(operator).execute(client);
            return Health.up()
                    .withDetail("enabled", true)
                    .withDetail("network", properties.network())
                    .build();
        } catch (Exception e) {
            return Health.down(e)
                    .withDetail("enabled", true)
                    .withDetail("network", properties.network())
                    .build();
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception ignored) {
                    // Best-effort cleanup only.
                }
            }
        }
    }
}
