/**
 * KMS module - scaffolded in E01 (Foundation), implemented in E05 (KMS/Key Lifecycle).
 *
 * <p>Owns key generation, storage, rotation, deactivation and provider abstraction
 * ({@code KeyProvider}: internal encrypted store first, then AWS KMS / Vault / HSM - see
 * Architecture &amp; Implementation Baseline, section 26). Private keys must never be exposed
 * through standard APIs. Corresponds to FR-KMS-* in the requirements catalogue.
 */
package com.idtrust.kms;
