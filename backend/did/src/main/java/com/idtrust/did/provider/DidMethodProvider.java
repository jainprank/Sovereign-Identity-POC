package com.idtrust.did.provider;

import com.idtrust.did.Did;

import java.util.Map;

/**
 * Method-specific DID lifecycle backend (FR-DID-* - "DIDMethodProvider abstraction (did:key,
 * did:web, ...)"). {@code create}/{@code resolve}/{@code update}/{@code deactivate} match the
 * operation names the spec's architecture section gives for this abstraction.
 *
 * <p>{@code update} rotates the DID's signing key and returns the updated DID Document
 * (FR-DID-007/FR-KMS-004 - key rotation). Whether rotation mints a new DID or updates the
 * existing one is not specified anywhere in the spec; this was resolved per-method: did:web
 * updates its existing DID Document's verification method in place (true rotation, same
 * identifier), while did:key - whose identifier is cryptographically derived from its own public
 * key - rejects rotation outright, since "rotating" it without changing the identifier isn't
 * cryptographically meaningful.
 */
public interface DidMethodProvider {

    DidMethod method();

    DidProviderResult create(DidCreationRequest request);

    /** Pure computation for did:key (no stored state needed); a live HTTPS fetch for did:web. */
    Map<String, Object> resolve(String did);

    DidProviderResult update(Did did);

    void deactivate(Did did);
}
