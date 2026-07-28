package com.idtrust.did.provider;

import java.util.List;
import java.util.Map;

/**
 * Builds a minimal W3C DID Document for a single Ed25519 verification method, shared by both
 * {@link com.idtrust.did.provider.didkey.DidKeyProvider} and
 * {@link com.idtrust.did.provider.didweb.DidWebProvider}. Ed25519 is the only signing algorithm
 * used for DID verification methods in this pass (a scope decision, not a spec requirement): the
 * spec gives no guidance on encoding non-Ed25519 keys into a DID Document (that would require
 * JWK-based verification methods, e.g. for secp256k1/RSA), so supporting those is left for later
 * if a concrete requirement emerges.
 */
public final class Ed25519DidDocuments {

    private Ed25519DidDocuments() {
    }

    public static Map<String, Object> build(String did, String publicKeyMultibase) {
        String verificationMethodId = did + "#" + publicKeyMultibase;
        Map<String, Object> verificationMethod = Map.of(
                "id", verificationMethodId,
                "type", "Ed25519VerificationKey2020",
                "controller", did,
                "publicKeyMultibase", publicKeyMultibase
        );
        return Map.of(
                "@context", List.of(
                        "https://www.w3.org/ns/did/v1",
                        "https://w3id.org/security/suites/ed25519-2020/v1"),
                "id", did,
                "verificationMethod", List.of(verificationMethod),
                "authentication", List.of(verificationMethodId),
                "assertionMethod", List.of(verificationMethodId)
        );
    }
}
