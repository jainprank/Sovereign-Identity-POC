package com.idtrust.hedera;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.hedera.config.HederaProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-HED-009 (Transaction Status), FR-HED-010 (Retry Failed Transaction) and FR-HED-012
 * (Anchoring Verification). Anchoring itself (FR-HED-004 through 008/011) happens via
 * {@code HederaAnchorPublisher.anchor(...)}, called directly by {@code trust}/{@code did}/
 * {@code credential} - this service only exposes visibility and remediation over anchors already
 * recorded by that publisher.
 */
@Service
public class HederaAnchorService {

    private static final Duration MIRROR_NODE_TIMEOUT = Duration.ofSeconds(5);

    private final HederaAnchorRepository anchorRepository;
    private final HcsHederaAnchorPublisher publisher;
    private final HederaProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(MIRROR_NODE_TIMEOUT)
            .build();

    public HederaAnchorService(HederaAnchorRepository anchorRepository, HcsHederaAnchorPublisher publisher,
                                HederaProperties properties, ObjectMapper objectMapper) {
        this.anchorRepository = anchorRepository;
        this.publisher = publisher;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** FR-HED-009 - a single anchor's current status. */
    public HederaAnchor get(UUID tenantId, UUID id) {
        return anchorRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Hedera anchor not found: " + id));
    }

    /** FR-HED-009 - every anchoring attempt recorded for a given resource, e.g. a DID or credential. */
    public List<HederaAnchor> listForResource(UUID tenantId, String resourceType, UUID resourceId) {
        return anchorRepository.findByTenantIdAndResourceTypeAndResourceId(tenantId, resourceType, resourceId);
    }

    /** FR-HED-010 - re-attempts a submission; only legal from {@code FAILED}, mirroring {@code TrustedIssuer.updateStatus}'s guard pattern. */
    @Transactional
    public HederaAnchor retry(UUID tenantId, UUID id) {
        HederaAnchor anchor = get(tenantId, id);
        if (anchor.getStatus() != AnchorStatus.FAILED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only a FAILED anchor can be retried: " + id);
        }
        publisher.retry(anchor);
        anchorRepository.save(anchor);
        return anchor;
    }

    /**
     * FR-HED-012 - fetches the actual on-chain message from the Hedera Mirror Node's public REST
     * API (a genuine live HTTPS round-trip, mirroring {@code did.provider.didweb.DidWebProvider}'s
     * precedent for resolving external state) and confirms its {@code resourceHash} still matches
     * what was anchored, detecting any local/remote divergence.
     */
    public boolean verify(UUID tenantId, UUID id) {
        HederaAnchor anchor = get(tenantId, id);
        if (anchor.getStatus() != AnchorStatus.CONFIRMED || anchor.getTopicSequenceNumber() == null) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only a CONFIRMED anchor can be verified: " + id);
        }
        URI uri = URI.create("https://" + properties.network() + ".mirrornode.hedera.com/api/v1/topics/"
                + properties.topicId() + "/messages/" + anchor.getTopicSequenceNumber());
        try {
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(MIRROR_NODE_TIMEOUT).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException(ApiErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Mirror Node lookup failed for anchor " + id + ": HTTP " + response.statusCode());
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> mirrorResponse = objectMapper.readValue(response.body(), Map.class);
            String base64Message = (String) mirrorResponse.get("message");
            String messageJson = new String(Base64.getDecoder().decode(base64Message), StandardCharsets.UTF_8);
            @SuppressWarnings("unchecked")
            Map<String, Object> envelope = objectMapper.readValue(messageJson, Map.class);
            return anchor.getResourceHash().equals(envelope.get("resourceHash"));
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(ApiErrorCode.EXTERNAL_SERVICE_ERROR,
                    "Mirror Node lookup failed for anchor " + id + ": " + e.getMessage());
        }
    }
}
