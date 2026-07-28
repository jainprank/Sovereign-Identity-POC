package com.idtrust.credential.schema;

import java.util.List;

/**
 * The full claim catalogue for a {@link CredentialSchema}, persisted as the {@code schema} JSONB
 * column. A single top-level {@code claims} list is sufficient for every requirement in EPIC 5
 * (Credential Schemas); nothing calls for schema-level metadata beyond what {@link
 * CredentialSchema} already carries as columns (name/version/format/status).
 */
public record SchemaDefinition(List<ClaimDefinition> claims) {
}
