package com.idtrust.apiplatform.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class WebhookDtos {

    private WebhookDtos() {
    }

    public record CreateRequest(
            @NotBlank String url,
            @NotEmpty List<String> eventTypes
    ) {
    }

    /** {@code secret} is populated exactly once, at creation time - see {@link WebhookEndpoint}'s Javadoc. */
    public record CreateResponse(WebhookEndpointResponse endpoint, String secret) {
        public static CreateResponse from(WebhookEndpointService.WebhookEndpointCreationResult result) {
            return new CreateResponse(WebhookEndpointResponse.from(result.endpoint()), result.secret());
        }
    }

    public record WebhookEndpointResponse(
            UUID id,
            String url,
            List<String> eventTypes,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static WebhookEndpointResponse from(WebhookEndpoint endpoint) {
            return new WebhookEndpointResponse(
                    endpoint.getId(),
                    endpoint.getUrl(),
                    endpoint.getEventTypes(),
                    endpoint.isActive(),
                    endpoint.getCreatedAt(),
                    endpoint.getUpdatedAt()
            );
        }
    }

    public record WebhookDeliveryResponse(
            UUID id,
            UUID webhookEndpointId,
            String eventType,
            String resourceType,
            UUID resourceId,
            WebhookDeliveryStatus status,
            int attempts,
            String lastError,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static WebhookDeliveryResponse from(WebhookDelivery delivery) {
            return new WebhookDeliveryResponse(
                    delivery.getId(),
                    delivery.getWebhookEndpointId(),
                    delivery.getEventType(),
                    delivery.getResourceType(),
                    delivery.getResourceId(),
                    delivery.getStatus(),
                    delivery.getAttempts(),
                    delivery.getLastError(),
                    delivery.getCreatedAt(),
                    delivery.getUpdatedAt()
            );
        }
    }
}
