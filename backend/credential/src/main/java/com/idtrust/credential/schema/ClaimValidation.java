package com.idtrust.credential.schema;

/**
 * Optional validation rules for a single claim (FR-SCH-010 - Define Validation Rules). Fields are
 * nullable and interpreted per {@link ClaimType} (e.g. min/maxLength for STRING, min/maxValue for
 * NUMBER) by whatever validates claims at issuance time (E07); this module only stores and
 * round-trips the rules.
 */
public record ClaimValidation(
        String pattern,
        Integer minLength,
        Integer maxLength,
        Double minValue,
        Double maxValue
) {
}
