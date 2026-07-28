package com.idtrust.hedera;

import java.time.Instant;
import java.util.UUID;

public final class HederaDtos {

    private HederaDtos() {
    }

    public record AnchorResponse(
            UUID id,
            String eventType,
            String resourceType,
            UUID resourceId,
            String resourceHash,
            AnchorStatus status,
            String transactionId,
            Long topicSequenceNumber,
            Instant consensusTimestamp,
            String failureReason,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static AnchorResponse from(HederaAnchor anchor) {
            return new AnchorResponse(
                    anchor.getId(),
                    anchor.getEventType(),
                    anchor.getResourceType(),
                    anchor.getResourceId(),
                    anchor.getResourceHash(),
                    anchor.getStatus(),
                    anchor.getTransactionId(),
                    anchor.getTopicSequenceNumber(),
                    anchor.getConsensusTimestamp(),
                    anchor.getFailureReason(),
                    anchor.getCreatedAt(),
                    anchor.getUpdatedAt()
            );
        }
    }

    public record VerifyResponse(UUID anchorId, boolean verified) {
    }

    public record NetworkResponse(
            UUID id,
            String networkName,
            String mirrorNodeUrl,
            String consensusNodeEndpoint,
            boolean active
    ) {
        public static NetworkResponse from(HederaNetwork network) {
            return new NetworkResponse(
                    network.getId(),
                    network.getNetworkName(),
                    network.getMirrorNodeUrl(),
                    network.getConsensusNodeEndpoint(),
                    network.isActive()
            );
        }
    }
}
