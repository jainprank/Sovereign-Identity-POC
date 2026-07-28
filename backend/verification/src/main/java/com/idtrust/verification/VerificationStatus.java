package com.idtrust.verification;

/**
 * Lifecycle status of a {@link VerificationRequest} (FR-VER-003 - Track Verification Status).
 * {@code PENDING} until a wallet calls {@code VerificationService#presentAndVerify}, which
 * resolves it to either {@code VERIFIED} or {@code FAILED}; {@code EXPIRED} is applied lazily
 * (mirroring E08's {@code CredentialStatus.EXPIRED} pattern) when a wallet presents against - or a
 * caller polls - a request past its {@code expiresAt}.
 */
public enum VerificationStatus {
    PENDING,
    VERIFIED,
    FAILED,
    EXPIRED
}
