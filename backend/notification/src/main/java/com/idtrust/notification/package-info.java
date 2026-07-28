/**
 * Notification module - implemented in E15, corresponding to EPIC 17 in the Architecture &amp;
 * Implementation Baseline (FR-NOT-001 through 006).
 *
 * <p>Provides the platform's user-facing notification surface: persisted in-app notifications
 * (FR-NOT-002, {@link com.idtrust.notification.Notification}/{@link com.idtrust.notification.NotificationController})
 * and best-effort email delivery (FR-NOT-001, {@link com.idtrust.notification.email.EmailNotificationService}),
 * both driven from a single choke point,
 * {@link com.idtrust.notification.NotificationEventPublisherImpl} - the sole real implementation
 * of {@link com.idtrust.common.notification.NotificationEventPublisher}, mirroring
 * {@code apiplatform.webhook.WebhookEventPublisherImpl}'s "interface in {@code common}, one real
 * impl in its owning module" pattern exactly.
 *
 * <p>{@code CredentialIssuanceService#issue} and {@code VerificationService#presentAndVerify} are
 * the two call sites (FR-NOT-003 Issuance Notification, FR-NOT-004 Verification Notification).
 * FR-NOT-005 (Webhook Notification) requires no new code here - it is already fully satisfied by
 * E14's {@code apiplatform.webhook.WebhookEventPublisherImpl}, a genuinely different delivery
 * mechanism (outbound HTTP to a tenant-configured endpoint) from this module's human-facing
 * email/in-app notifications. FR-NOT-006 (Notification Preferences) is V1-priority and out of
 * scope for this pass - see the root README's E15 section for the documented rationale.
 */
package com.idtrust.notification;
