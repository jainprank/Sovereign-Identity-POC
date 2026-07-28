package com.idtrust.verification.policy;

import java.util.List;

/**
 * The full rule set for a {@link VerificationPolicy}, persisted as the {@code policy} JSONB
 * column (EPIC 12, FR-POL-006 through FR-POL-011). {@code schemaId} (FR-POL-006 - Define Schema
 * Requirement) is nullable: a policy may accept any schema and rely solely on
 * {@code requiredClaims}/{@code trustedIssuerDids}. {@code trustedIssuerDids} (FR-POL-011/
 * FR-VER-012 - Trust Rule/Verify Trust) is a static allowlist stand-in for the full Trust Registry
 * (EPIC 13/FR-TRUST-*), which is out of scope until E12 per the build sequence - an empty list
 * means "trust any issuer", matching the Baseline's "trust rules are optional" framing rather than
 * failing closed for policies that don't care about issuer trust.
 */
public record PolicyDefinition(
        java.util.UUID schemaId,
        List<String> requiredClaims,
        List<String> trustedIssuerDids,
        boolean requireActiveStatus,
        boolean requireNotExpired
) {
}
