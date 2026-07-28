package com.idtrust.credential.issuance;

/**
 * Lifecycle status of an issued credential (FR-ISS-*, FR-CRED-*). Only {@link #ISSUED} is
 * reachable by this epic (E07); {@link #SUSPENDED}, {@link #REVOKED} and {@link #EXPIRED} are
 * reserved for E08 (Status &amp; Revocation), mirroring the "reserve enum values for future
 * epics" pattern already used by {@code CredentialFormat}/{@code KeyProviderType}/{@code DidMethod}.
 */
public enum CredentialStatus {
    ISSUED,
    SUSPENDED,
    REVOKED,
    EXPIRED
}
