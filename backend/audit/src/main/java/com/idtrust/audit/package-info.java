/**
 * Audit &amp; Compliance module - implemented in E15, corresponding to EPIC 15 in the Architecture
 * &amp; Implementation Baseline (FR-AUD-001 through 012).
 *
 * <p>FR-AUD-001 through 008 (Authentication/Authorization/DID/Key/Credential/Issuance/
 * Verification/Trust Audit) require no new code here: every domain module has published a
 * {@link com.idtrust.common.audit.AuditEvent} via {@link com.idtrust.common.audit.AuditEventPublisher}
 * since its own epic (E02's login/role-change events, E04's DID lifecycle, E05's key lifecycle,
 * E06-E08's credential/issuance events, E10's verification events, E12's trust registry events).
 * Until now those events were only ever logged ({@code common.audit.LoggingAuditEventPublisher})
 * or relayed as a webhook - never persisted anywhere queryable. This module closes that gap:
 * {@link com.idtrust.audit.PersistingAuditEventPublisher} is the new primary
 * {@code AuditEventPublisher} bean, durably storing every event in {@code audit_records}
 * (FR-AUD-009 Search Audit, via {@link com.idtrust.audit.AuditController#search}) and exposing a
 * CSV export (FR-AUD-010 Export Audit, via {@link com.idtrust.audit.AuditController#export}).
 *
 * <p>FR-AUD-011 (Retention Policy) and FR-AUD-012 (Data Deletion) are V1-priority in the Baseline
 * and out of scope for this pass - see the root README's E15 section for the documented
 * rationale.
 */
package com.idtrust.audit;
