package com.idtrust.hedera;

/**
 * Terminal outcome of a {@link HederaAnchor} attempt. There is no transient PENDING/SUBMITTED
 * state - the Hedera Java SDK's {@code TopicMessageSubmitTransaction.execute(...).getReceipt(...)}
 * call blocks for consensus, so by the time {@code HcsHederaAnchorPublisher.anchor(...)} returns,
 * the terminal outcome is already known.
 */
public enum AnchorStatus {

    /** Submitted to HCS and consensus was reached; {@code transactionId}/{@code topicSequenceNumber}/{@code consensusTimestamp} are populated. */
    CONFIRMED,

    /** Submission was attempted (Hedera is enabled) but failed - network error, bad credentials, SDK error; see {@code failureReason}. Retryable via FR-HED-010. */
    FAILED,

    /** {@code idtrust.hedera.enabled=false} - no network call was attempted. Not retryable until Hedera is enabled for this tenant/environment. */
    DISABLED
}
