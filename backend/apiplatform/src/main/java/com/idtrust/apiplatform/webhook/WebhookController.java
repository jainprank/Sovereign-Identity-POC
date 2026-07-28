package com.idtrust.apiplatform.webhook;

import com.idtrust.common.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Tenant-scoped management of {@link WebhookEndpoint}s and their {@link WebhookDelivery}s
 * (FR-API-008/009). Restricted to PLATFORM_ADMIN/ORG_ADMIN, mirroring
 * {@code client.ApiClientController} - registering an outbound webhook (with its signing secret)
 * is a privileged operation.
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
public class WebhookController {

    private final WebhookEndpointService webhookEndpointService;
    private final WebhookDeliveryService webhookDeliveryService;

    public WebhookController(WebhookEndpointService webhookEndpointService,
                              WebhookDeliveryService webhookDeliveryService) {
        this.webhookEndpointService = webhookEndpointService;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WebhookDtos.CreateResponse create(@Valid @RequestBody WebhookDtos.CreateRequest request) {
        WebhookEndpointService.WebhookEndpointCreationResult result = webhookEndpointService.create(
                TenantContext.requireTenantId(), request.url(), request.eventTypes());
        return WebhookDtos.CreateResponse.from(result);
    }

    @GetMapping
    public List<WebhookDtos.WebhookEndpointResponse> list() {
        return webhookEndpointService.list(TenantContext.requireTenantId()).stream()
                .map(WebhookDtos.WebhookEndpointResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WebhookDtos.WebhookEndpointResponse get(@PathVariable UUID id) {
        return WebhookDtos.WebhookEndpointResponse.from(webhookEndpointService.get(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/deactivate")
    public WebhookDtos.WebhookEndpointResponse deactivate(@PathVariable UUID id) {
        return WebhookDtos.WebhookEndpointResponse.from(
                webhookEndpointService.deactivate(TenantContext.requireTenantId(), id));
    }

    @PostMapping("/{id}/activate")
    public WebhookDtos.WebhookEndpointResponse activate(@PathVariable UUID id) {
        return WebhookDtos.WebhookEndpointResponse.from(
                webhookEndpointService.activate(TenantContext.requireTenantId(), id));
    }

    @GetMapping("/{id}/deliveries")
    public List<WebhookDtos.WebhookDeliveryResponse> listDeliveries(@PathVariable UUID id) {
        return webhookDeliveryService.listForEndpoint(TenantContext.requireTenantId(), id).stream()
                .map(WebhookDtos.WebhookDeliveryResponse::from).toList();
    }

    @GetMapping("/deliveries/{deliveryId}")
    public WebhookDtos.WebhookDeliveryResponse getDelivery(@PathVariable UUID deliveryId) {
        return WebhookDtos.WebhookDeliveryResponse.from(
                webhookDeliveryService.get(TenantContext.requireTenantId(), deliveryId));
    }

    @PostMapping("/deliveries/{deliveryId}/retry")
    public WebhookDtos.WebhookDeliveryResponse retryDelivery(@PathVariable UUID deliveryId) {
        return WebhookDtos.WebhookDeliveryResponse.from(
                webhookDeliveryService.retry(TenantContext.requireTenantId(), deliveryId));
    }
}
