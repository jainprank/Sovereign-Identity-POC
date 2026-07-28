package com.idtrust.common.hedera;

/**
 * Publishes {@link HederaAnchorRequest}s for anchoring to Hedera Consensus Service. Mirrors
 * {@code AuditEventPublisher}'s decoupling: every domain module calls this synchronously and
 * unconditionally after a state-changing operation, but implementations must never throw and
 * must never block identity/credential functionality if Hedera or the {@code hedera} module is
 * unavailable (package-info.java's explicit requirement for the {@code hedera} module).
 *
 * <p>The single real implementation ({@code HcsHederaAnchorPublisher} in the {@code hedera}
 * module) persists a terminal {@code CONFIRMED}/{@code FAILED}/{@code DISABLED} anchor record for
 * every call - never a silent no-op - so anchoring history remains queryable (FR-HED-009) even
 * when {@code idtrust.hedera.enabled=false} or the network is unreachable.
 */
public interface HederaAnchorPublisher {

    void anchor(HederaAnchorRequest request);
}
