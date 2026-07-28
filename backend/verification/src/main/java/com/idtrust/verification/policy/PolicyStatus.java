package com.idtrust.verification.policy;

/**
 * Lifecycle status of a {@link VerificationPolicy} (EPIC 12/FR-POL-004 - Activate Policy,
 * FR-POL-005 - Deactivate Policy). Mirrors {@code credential.schema.SchemaStatus}'s
 * versioned-by-row pattern: a policy starts {@code DRAFT} (freely editable), becomes
 * {@code ACTIVE} (immutable - verification requests may reference it) and can later be
 * {@code INACTIVE} (no longer usable for new verification requests, but past requests that
 * already reference it remain valid history).
 */
public enum PolicyStatus {
    DRAFT,
    ACTIVE,
    INACTIVE
}
