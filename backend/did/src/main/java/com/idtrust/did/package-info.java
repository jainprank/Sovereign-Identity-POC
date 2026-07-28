/**
 * DID module - scaffolded in E01 (Foundation), implemented in E04 (DID).
 *
 * <p>Owns DID lifecycle (create, import, resolve, update, deactivate, rotate) behind a
 * {@code DIDMethodProvider} abstraction (did:key, did:web, ... - see Architecture &amp;
 * Implementation Baseline, section 27). Depends on {@code kms} for signing keys.
 * Corresponds to FR-DID-* in the requirements catalogue.
 */
package com.idtrust.did;
