package com.idtrust.credential.schema;

/**
 * Supported claim data types (FR-SCH-009 - Define Claim Data Types). Kept to the small set the
 * Baseline's own examples use (all string/number-ish JSON claim values); richer types (object,
 * array) aren't called for by any requirement yet and would be premature scope.
 */
public enum ClaimType {
    STRING,
    NUMBER,
    BOOLEAN,
    DATE
}
