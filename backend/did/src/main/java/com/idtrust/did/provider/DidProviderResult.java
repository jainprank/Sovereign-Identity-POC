package com.idtrust.did.provider;

import java.util.Map;
import java.util.UUID;

/**
 * @param did      the DID identifier (method-specific format).
 * @param document the DID Document, as a directly JSON-serializable structure.
 * @param keyId    the {@code com.idtrust.kms.key.ManagedKey} id backing this DID's verification
 *                 method after this operation (newly generated on create, rotated on update).
 */
public record DidProviderResult(String did, Map<String, Object> document, UUID keyId) {
}
