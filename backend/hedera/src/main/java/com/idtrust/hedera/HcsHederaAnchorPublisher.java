package com.idtrust.hedera;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hedera.hashgraph.sdk.AccountId;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.TopicId;
import com.hedera.hashgraph.sdk.TopicMessageSubmitTransaction;
import com.hedera.hashgraph.sdk.TransactionReceipt;
import com.hedera.hashgraph.sdk.TransactionRecord;
import com.hedera.hashgraph.sdk.TransactionResponse;
import com.idtrust.common.hedera.HederaAnchorPublisher;
import com.idtrust.common.hedera.HederaAnchorRequest;
import com.idtrust.hedera.config.HederaProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The single real {@code HederaAnchorPublisher} bean (FR-HED-004 through 008, 011). Every domain
 * module ({@code trust}, {@code did}, {@code credential}) calls {@link #anchor} synchronously
 * right after its own state-changing operation commits - this is the pragmatic, precedent-matching
 * substitute for the Baseline's literal Kafka-based async design (section 17): no message broker
 * exists anywhere in this codebase (see {@code AuditEventPublisher}'s identical synchronous-but-
 * decoupled pattern), but callers remain fully decoupled from Hedera availability because this
 * method never throws - any failure (disabled, unreachable network, bad credentials, SDK error) is
 * captured as a persisted {@link HederaAnchor} row instead of propagating (FR-HED-011's "async" in
 * spirit: the caller is never blocked from completing on Hedera's account).
 *
 * <p>When {@code idtrust.hedera.enabled=false} (the default - see {@link HederaProperties}), no
 * network/gRPC call is ever attempted; a {@code DISABLED} row is persisted instead, so identity and
 * credential functionality remain fully operational without a real Hedera account, per the
 * {@code hedera} module's package-info.java.
 */
@Component
public class HcsHederaAnchorPublisher implements HederaAnchorPublisher {

    private static final Logger log = LoggerFactory.getLogger(HcsHederaAnchorPublisher.class);

    private final HederaAnchorRepository anchorRepository;
    private final HederaProperties properties;
    private final ObjectMapper objectMapper;
    private final Client client;

    public HcsHederaAnchorPublisher(HederaAnchorRepository anchorRepository, HederaProperties properties,
                                     ObjectMapper objectMapper) {
        this.anchorRepository = anchorRepository;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = properties.enabled() ? buildClient(properties) : null;
    }

    @Override
    public void anchor(HederaAnchorRequest request) {
        HederaAnchor anchorRecord = new HederaAnchor(request.tenantId(), request.eventType(), request.resourceType(),
                request.resourceId(), request.resourceHash());
        submitOrDisable(anchorRecord);
        anchorRepository.save(anchorRecord);
    }

    /** FR-HED-010 - re-attempts a previously {@code FAILED} anchor. Legality is guarded by {@code HederaAnchorService}. */
    void retry(HederaAnchor anchorRecord) {
        submitOrDisable(anchorRecord);
    }

    private void submitOrDisable(HederaAnchor anchorRecord) {
        if (!properties.enabled() || client == null) {
            anchorRecord.markDisabled();
            return;
        }
        try {
            String envelope = objectMapper.writeValueAsString(envelopeOf(anchorRecord));
            TransactionResponse response = new TopicMessageSubmitTransaction()
                    .setTopicId(TopicId.fromString(properties.topicId()))
                    .setMessage(envelope)
                    .execute(client);
            TransactionReceipt receipt = response.getReceipt(client);
            TransactionRecord record = response.getRecord(client);
            anchorRecord.markConfirmed(response.transactionId.toString(), receipt.topicSequenceNumber,
                    record.consensusTimestamp);
        } catch (Exception e) {
            log.warn("Hedera anchor submission failed for {} {}: {}",
                    anchorRecord.getResourceType(), anchorRecord.getResourceId(), e.getMessage());
            anchorRecord.markFailed(e.getMessage());
        }
    }

    /** The on-chain envelope: only hashes/metadata, never {@code tenantId}, PII or raw content (Baseline section 26, Decision 4). */
    private Map<String, Object> envelopeOf(HederaAnchor anchorRecord) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("eventType", anchorRecord.getEventType());
        envelope.put("resourceType", anchorRecord.getResourceType());
        envelope.put("resourceId", anchorRecord.getResourceId().toString());
        envelope.put("resourceHash", anchorRecord.getResourceHash());
        envelope.put("timestamp", Instant.now().toString());
        return envelope;
    }

    private static Client buildClient(HederaProperties properties) {
        try {
            Client client = Client.forName(properties.network());
            client.setOperator(AccountId.fromString(properties.accountId()), PrivateKey.fromString(properties.operatorKey()));
            return client;
        } catch (Exception e) {
            log.warn("Failed to initialize Hedera client (network={}): {} - anchoring will behave as disabled",
                    properties.network(), e.getMessage());
            return null;
        }
    }
}
