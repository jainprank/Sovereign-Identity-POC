package com.idtrust.trust;

/**
 * Trust status of a single {@link TrustedIssuer} entry (FR-TRUST-004 - Update Trust Status,
 * FR-TRUST-005 - Revoke Trust). {@code TRUSTED}/{@code SUSPENDED} are reversible (an admin can
 * toggle back and forth, e.g. while investigating a report); {@code REVOKED} is terminal, mirroring
 * {@code credential.issuance.Credential#revoke}'s "revocation never reverses" precedent from E08 -
 * a permanently distrusted issuer must be re-registered from scratch, not reinstated.
 */
public enum TrustStatus {
    TRUSTED,
    SUSPENDED,
    REVOKED
}
