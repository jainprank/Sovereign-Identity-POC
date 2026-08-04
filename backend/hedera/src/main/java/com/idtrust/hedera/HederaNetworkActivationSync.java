package com.idtrust.hedera;

import com.idtrust.hedera.config.HederaProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Keeps {@code hedera_networks.is_active} in sync with whichever network this instance is
 * actually configured to anchor to ({@code idtrust.hedera.network}). Without this, the reference
 * catalogue's seeded {@code is_active} flags (both {@code testnet} and {@code mainnet} start
 * {@code true} - see {@code V41__hedera_networks_core.sql}) don't reflect reality, and consumers
 * that pick "the active network" (e.g. the admin console's HashScan links via
 * {@code GET /api/v1/hedera/networks}) can point at a network the anchor transaction was never
 * submitted to, landing on a dead/wrong-network HashScan page instead of the real transaction.
 */
@Component
public class HederaNetworkActivationSync implements ApplicationRunner {

    private final HederaNetworkRepository networkRepository;
    private final HederaProperties properties;

    public HederaNetworkActivationSync(HederaNetworkRepository networkRepository, HederaProperties properties) {
        this.networkRepository = networkRepository;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (HederaNetwork network : networkRepository.findAll()) {
            boolean shouldBeActive = network.getNetworkName().equalsIgnoreCase(properties.network());
            if (network.isActive() != shouldBeActive) {
                network.setActive(shouldBeActive);
                networkRepository.save(network);
            }
        }
    }
}
