/**
 * Hedera Trust Adapter module - scaffolded in E01 (Foundation), implemented in E13 (Hedera).
 *
 * <p>Implements a {@code TrustAdapter} for anchoring hashes/timestamps (trust registry entries,
 * DID anchoring, schema hashes, credential integrity hashes) to Hedera Consensus Service.
 * Identity and credential functionality must remain fully operational if this module or the
 * Hedera network is unavailable - anchoring is asynchronous and never stores PII, raw
 * credentials, or private keys on-chain. Corresponds to FR-HED-* in the requirements catalogue.
 */
package com.idtrust.hedera;
