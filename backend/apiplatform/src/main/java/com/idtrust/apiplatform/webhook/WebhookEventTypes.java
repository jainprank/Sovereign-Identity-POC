package com.idtrust.apiplatform.webhook;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;

import java.util.List;
import java.util.Set;

/**
 * The fixed vocabulary a {@link WebhookEndpoint} may subscribe to (FR-API-008) - exactly the
 * Kafka event-type names already catalogued in the Architecture &amp; Implementation Baseline,
 * section 6 ("Inter-Service Communication"), reused here rather than invented afresh, since no
 * Kafka broker exists anywhere in this codebase yet.
 */
public final class WebhookEventTypes {

    public static final Set<String> VALID_EVENT_TYPES = Set.of(
            "CredentialIssued", "CredentialRevoked", "CredentialSuspended", "VerificationCompleted",
            "DIDCreated", "DIDUpdated", "TrustRegistryUpdated", "HederaAnchorRequested",
            "HederaAnchorCompleted", "AuditEventCreated");

    private WebhookEventTypes() {
    }

    public static void validate(List<String> eventTypes) {
        if (eventTypes == null || eventTypes.isEmpty()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "At least one event type is required");
        }
        for (String eventType : eventTypes) {
            if (!VALID_EVENT_TYPES.contains(eventType)) {
                throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                        "Unknown event type: " + eventType + " - must be one of " + VALID_EVENT_TYPES);
            }
        }
    }
}
