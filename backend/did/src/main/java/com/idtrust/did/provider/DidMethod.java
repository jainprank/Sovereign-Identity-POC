package com.idtrust.did.provider;

/**
 * DID methods supported by the platform. Both are treated as MVP-scoped in this implementation:
 * the Master Specification's persona/scope table (section 4.2) lists did:web under "V1", but its
 * own AI-build-instruction sections and the Architecture & Implementation Baseline's MVP backlog
 * explicitly call for did:key AND did:web together up front - this contradiction is resolved in
 * favor of building both now.
 */
public enum DidMethod {
    KEY,
    WEB
}
