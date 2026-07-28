package com.idtrust.credential.schema;

/**
 * Optional wallet/UI display metadata for a single claim (FR-SCH-011 - Define Display Metadata).
 */
public record ClaimDisplay(String label, Integer order) {
}
