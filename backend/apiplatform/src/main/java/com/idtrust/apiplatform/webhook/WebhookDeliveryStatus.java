package com.idtrust.apiplatform.webhook;

/** Terminal outcome of a single {@link WebhookDelivery} attempt (FR-API-008/009). */
public enum WebhookDeliveryStatus {

    /** The HTTP delivery attempt received a 2xx response. */
    DELIVERED,

    /** The HTTP delivery attempt failed (network error, non-2xx response); see {@code lastError}. Retryable (FR-API-009). */
    FAILED
}
