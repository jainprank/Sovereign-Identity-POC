package com.idtrust.credential.schema;

/**
 * A single claim in a {@link SchemaDefinition} (FR-SCH-007 - Define Required Claims, FR-SCH-008 -
 * Define Optional Claims, FR-SCH-009 - Define Claim Data Types). {@code validation} and
 * {@code display} are optional (FR-SCH-010/011).
 */
public record ClaimDefinition(
        String name,
        ClaimType type,
        boolean required,
        String description,
        ClaimValidation validation,
        ClaimDisplay display
) {
}
