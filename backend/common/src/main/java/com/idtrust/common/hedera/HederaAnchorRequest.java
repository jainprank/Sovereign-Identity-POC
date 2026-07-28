package com.idtrust.common.hedera;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

/**
 * A request to anchor a hash of some platform resource to Hedera Consensus Service (FR-HED-004
 * through 008). Only {@link #resourceHash()} ever leaves this platform - callers must never pass
 * raw content, PII, credentials or private keys here (Baseline section 26, Decision 4).
 *
 * <p>{@code tenantId} is deliberately not part of the on-chain envelope published by the
 * {@code hedera} module - it stays here purely so the persisted {@code HederaAnchor} row can be
 * tenant-scoped/RLS-protected like every other table in this codebase.
 *
 * @param tenantId     tenant the anchored resource belongs to (used for local persistence only)
 * @param eventType    stable event name, e.g. {@code "CREDENTIAL_SCHEMA_ANCHORED"}
 * @param resourceType the type of resource anchored, e.g. {@code "DID"}, {@code "CREDENTIAL"}
 * @param resourceId   identifier of the anchored resource
 * @param resourceHash SHA-256 hex digest of the resource - never raw content
 */
public record HederaAnchorRequest(
        UUID tenantId,
        String eventType,
        String resourceType,
        UUID resourceId,
        String resourceHash
) {

    /**
     * Builds a request by hashing {@code rawContent} with SHA-256. Use this when the caller has
     * not already computed a hash of the resource (e.g. a DID document or schema definition).
     */
    public static HederaAnchorRequest ofRawContent(UUID tenantId, String eventType, String resourceType,
                                                    UUID resourceId, String rawContent) {
        return new HederaAnchorRequest(tenantId, eventType, resourceType, resourceId, sha256Hex(rawContent));
    }

    /**
     * Builds a request from a hash the caller already computed (e.g. {@code Credential.credentialHash},
     * computed once at issuance time). Avoids re-hashing an already-hashed value.
     */
    public static HederaAnchorRequest ofHash(UUID tenantId, String eventType, String resourceType,
                                              UUID resourceId, String resourceHash) {
        return new HederaAnchorRequest(tenantId, eventType, resourceType, resourceId, resourceHash);
    }

    private static String sha256Hex(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
