package com.idtrust.did;

/**
 * Lifecycle status of a DID (FR-DID-*). {@code DEACTIVATED} is a local, platform-side flag: for
 * did:key there is no external document to un-publish (resolution is pure computation from the
 * identifier), and for did:web the tenant is responsible for actually removing/updating the
 * hosted {@code did.json} - deactivating here only stops the platform from using the DID for new
 * signing/issuance operations.
 */
public enum DidStatus {
    ACTIVE,
    DEACTIVATED
}
