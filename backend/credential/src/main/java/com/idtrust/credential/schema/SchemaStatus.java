package com.idtrust.credential.schema;

/**
 * Lifecycle status of a {@link CredentialSchema} (FR-SCH-005 - Publish Schema, FR-SCH-006 -
 * Deprecate Schema). A schema starts as {@code DRAFT} (freely editable), becomes {@code
 * PUBLISHED} (immutable - issuance may reference it) and can later be {@code DEPRECATED}
 * (no longer intended for new issuance, but already-issued credentials still reference it).
 */
public enum SchemaStatus {
    DRAFT,
    PUBLISHED,
    DEPRECATED
}
