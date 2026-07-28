package com.idtrust.wallet.oid4vci;

/**
 * Lifecycle status of an {@link Oid4vciSession} (FR-OID4VCI-014 - Issuance Session).
 * {@code OFFERED} (offer created, pre-authorized code not yet exchanged) -&gt;
 * {@code TOKEN_ISSUED} (token endpoint called, access token issued) -&gt;
 * {@code CLAIMED} (credential endpoint called, VC handed to the wallet). {@code EXPIRED} is a
 * terminal state reached from either of the first two if the session's TTL lapses before the next
 * step.
 */
public enum Oid4vciSessionStatus {
    OFFERED,
    TOKEN_ISSUED,
    CLAIMED,
    EXPIRED
}
