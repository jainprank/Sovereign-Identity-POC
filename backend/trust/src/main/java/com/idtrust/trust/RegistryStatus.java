package com.idtrust.trust;

/**
 * Lifecycle status of a {@link TrustRegistry} itself (not the trust status of an individual
 * {@link TrustedIssuer} within it - see {@link TrustStatus} for that). Unlike
 * {@code credential.schema.CredentialSchema}/{@code verification.policy.VerificationPolicy}, a
 * trust registry has no DRAFT step - FR-TRUST-001 has no publish/freeze acceptance criteria, so a
 * registry is usable for issuer registration immediately upon creation.
 */
public enum RegistryStatus {
    ACTIVE,
    INACTIVE
}
