package com.idtrust.did.provider.didweb;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.did.Did;
import com.idtrust.did.provider.DidCreationRequest;
import com.idtrust.did.provider.DidMethod;
import com.idtrust.did.provider.DidMethodProvider;
import com.idtrust.did.provider.DidProviderResult;
import com.idtrust.did.provider.Ed25519DidDocuments;
import com.idtrust.did.provider.Ed25519Multibase;
import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.KeyType;
import com.idtrust.kms.key.ManagedKey;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * did:web (FR-DID-*): the identifier is derived from a domain (and optional path), per the
 * did:web spec - colons in the domain (ports) are percent-encoded, and path segments are
 * separated by colons instead of slashes. Unlike did:key, resolution requires a real HTTPS fetch
 * of the DID Document the tenant is expected to host at {@code https://<domain>/.well-known/did.json}
 * (no path) or {@code https://<domain>/<path>/did.json} (with path).
 *
 * <p>Rotation ({@link #update}) generates a new signing key and returns the DID Document with the
 * new verification method, updating the existing DID identifier in place - it is the tenant's
 * responsibility to publish the returned document at the same hosted URL afterward (the platform
 * has no access to the tenant's web server).
 */
@Component
public class DidWebProvider implements DidMethodProvider {

    private static final Duration RESOLUTION_TIMEOUT = Duration.ofSeconds(5);

    private final KeyService keyService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(RESOLUTION_TIMEOUT)
            .build();

    public DidWebProvider(KeyService keyService, ObjectMapper objectMapper) {
        this.keyService = keyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public DidMethod method() {
        return DidMethod.WEB;
    }

    @Override
    public DidProviderResult create(DidCreationRequest request) {
        if (request.domain() == null || request.domain().isBlank()) {
            throw new ApiException(ApiErrorCode.INVALID_DID, "did:web requires a domain");
        }
        ManagedKey key = keyService.generate(request.tenantId(), KeyType.SIGNING, KeyAlgorithm.ED25519, null);
        keyService.linkToDid(key.getId(), request.didId());

        String did = buildIdentifier(request.domain(), request.path());
        Map<String, Object> document = Ed25519DidDocuments.build(did, Ed25519Multibase.encode(key.getPublicKey()));
        return new DidProviderResult(did, document, key.getId());
    }

    @Override
    public Map<String, Object> resolve(String did) {
        URI documentUri = resolutionUri(did);
        try {
            HttpRequest request = HttpRequest.newBuilder(documentUri)
                    .timeout(RESOLUTION_TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new ApiException(ApiErrorCode.EXTERNAL_SERVICE_ERROR,
                        "Failed to resolve " + did + ": HTTP " + response.statusCode());
            }
            return objectMapper.readValue(response.body(), Map.class);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ApiException(ApiErrorCode.EXTERNAL_SERVICE_ERROR, "Failed to resolve " + did + ": " + e.getMessage());
        }
    }

    @Override
    public DidProviderResult update(Did did) {
        ManagedKey activeKey = keyService.getActiveKeyForDid(did.getId());
        ManagedKey rotated = keyService.rotate(activeKey.getId());
        Map<String, Object> document = Ed25519DidDocuments.build(did.getDid(), Ed25519Multibase.encode(rotated.getPublicKey()));
        return new DidProviderResult(did.getDid(), document, rotated.getId());
    }

    @Override
    public void deactivate(Did did) {
        // No-op: the platform doesn't control the tenant's web server, so it cannot un-publish
        // the hosted did.json. Deactivation is enforced locally via Did.status; the tenant is
        // responsible for actually removing or updating the hosted document.
    }

    private String buildIdentifier(String domain, String path) {
        String encodedDomain = domain.replace(":", "%3A");
        if (path == null || path.isBlank()) {
            return "did:web:" + encodedDomain;
        }
        String encodedPath = path.replaceAll("^/+|/+$", "").replace("/", ":");
        return "did:web:" + encodedDomain + ":" + encodedPath;
    }

    private URI resolutionUri(String did) {
        String prefix = "did:web:";
        if (!did.startsWith(prefix)) {
            throw new ApiException(ApiErrorCode.INVALID_DID, "Not a did:web identifier: " + did);
        }
        String[] segments = did.substring(prefix.length()).split(":");
        String domain = segments[0].replace("%3A", ":");
        if (segments.length == 1) {
            return URI.create("https://" + domain + "/.well-known/did.json");
        }
        StringBuilder path = new StringBuilder();
        for (int i = 1; i < segments.length; i++) {
            path.append('/').append(segments[i]);
        }
        return URI.create("https://" + domain + path + "/did.json");
    }
}
