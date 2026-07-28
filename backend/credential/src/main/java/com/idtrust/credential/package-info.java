/**
 * Credential module - scaffolded in E01 (Foundation), implemented across E06-E08:
 * <ul>
 *   <li>E06 - Credential schemas &amp; templates (FR-SCH-*)</li>
 *   <li>E07 - Credential issuance, W3C VC signing (FR-ISS-*)</li>
 *   <li>E08 - Credential status &amp; revocation (FR-CRED-*)</li>
 * </ul>
 *
 * <p>Uses a {@code CredentialFormatProvider} abstraction (W3C VC first, then SD-JWT VC, mDoc -
 * see Architecture &amp; Implementation Baseline, section 28) and depends on {@code did} and
 * {@code kms} for issuer resolution and signing.
 */
package com.idtrust.credential;
