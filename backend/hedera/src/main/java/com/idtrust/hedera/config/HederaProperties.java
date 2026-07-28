package com.idtrust.hedera.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds {@code idtrust.hedera.*} configuration (FR-HED-001 Network, FR-HED-002 Account,
 * FR-HED-003 HCS Topic Configuration).
 *
 * @param enabled    when {@code false} (the default), {@code HcsHederaAnchorPublisher} never
 *                    attempts a live network/gRPC call and persists a {@code DISABLED} anchor
 *                    record instead - identity/credential functionality must remain fully
 *                    operational without a real Hedera account (see the {@code hedera} module's
 *                    package-info.java). Every non-local environment that wants real anchoring
 *                    must override this via the {@code HEDERA_ENABLED} environment variable.
 * @param network    one of Hedera's named networks accepted by {@code Client.forName(...)}, e.g.
 *                    {@code "testnet"}, {@code "mainnet"}, {@code "previewnet"}.
 * @param accountId  the operator account (e.g. {@code "0.0.1234"}) that pays for and signs anchor
 *                    transactions.
 * @param operatorKey the operator account's private key (DER or raw hex), read from the
 *                    HEDERA_OPERATOR_KEY environment variable - never logged, never persisted.
 * @param topicId    the HCS topic (e.g. {@code "0.0.5678"}) that anchor messages are submitted to.
 *                    One topic per tenant/ecosystem is out of scope for V1 (FR-HED-014 Multi-Network
 *                    Configuration is V2) - a single shared topic is used for every tenant.
 */
@ConfigurationProperties(prefix = "idtrust.hedera")
public record HederaProperties(boolean enabled, String network, String accountId, String operatorKey, String topicId) {
}
