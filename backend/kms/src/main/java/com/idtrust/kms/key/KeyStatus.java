package com.idtrust.kms.key;

/**
 * Lifecycle status of a managed key (FR-KMS-*). {@code ACTIVE} keys may sign; {@code ROTATED}
 * keys have been superseded by a newer key (see {@code rotatedFromKeyId} on the successor) but
 * are retained for verifying signatures/credentials issued before rotation; {@code DEACTIVATED}
 * keys are disabled (e.g. suspected compromise) and must not be used for new signatures.
 */
public enum KeyStatus {
    ACTIVE,
    ROTATED,
    DEACTIVATED
}
