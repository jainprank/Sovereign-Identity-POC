package com.idtrust.trust;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implements EPIC 13 (Trust Registry): FR-TRUST-001 (Create Trust Registry), FR-TRUST-002/003
 * (Register Issuer/Register DID, folded into one call - see {@link #registerIssuer} and
 * {@link TrustedIssuer}'s Javadoc), FR-TRUST-004 (Update Trust Status), FR-TRUST-005 (Revoke
 * Trust), FR-TRUST-006 (Trust Lookup), FR-TRUST-007 (Trust Check, the dynamic counterpart to
 * {@code verification.policy.PolicyDefinition#trustedIssuerDids}'s static allowlist - see
 * {@link #isTrusted} and {@code verification.VerificationService}'s class Javadoc for how the two
 * are combined), and FR-TRUST-011 (Trust Audit, via the same {@code AuditEventPublisher} pattern
 * every other module uses).
 *
 * <p>Deliberate scope decisions:
 * <ul>
 *   <li>FR-TRUST-008 (Trust Framework) is a plain free-text field on {@link TrustRegistry}, not a
 *       dedicated typed entity with its own governance rules - V1-priority, out of scope.</li>
 *   <li>FR-TRUST-009/010 (Trust List Import/Export, e.g. bulk-loading an external eIDAS/OpenID
 *       Federation trust list) are V1-priority and out of scope, mirroring E06's precedent of
 *       skipping schema import/export.</li>
 *   <li>FR-TRUST-012 (Hedera Anchoring) is explicitly deferred to E13 (Hedera) per the build
 *       sequence - registering/revoking trust here has no on-chain side effect yet.</li>
 * </ul>
 */
@Service
public class TrustRegistryService {

    private final TrustRegistryRepository registryRepository;
    private final TrustedIssuerRepository issuerRepository;
    private final AuditEventPublisher auditEventPublisher;

    public TrustRegistryService(TrustRegistryRepository registryRepository, TrustedIssuerRepository issuerRepository,
                                 AuditEventPublisher auditEventPublisher) {
        this.registryRepository = registryRepository;
        this.issuerRepository = issuerRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public TrustRegistry create(UUID tenantId, String name, String framework) {
        TrustRegistry registry = new TrustRegistry(tenantId, name, framework);
        registryRepository.save(registry);
        audit(tenantId, "TRUST_REGISTRY_CREATED", "TRUST_REGISTRY", registry.getId());
        return registry;
    }

    public TrustRegistry get(UUID tenantId, UUID id) {
        return registryRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Trust registry not found: " + id));
    }

    public List<TrustRegistry> list(UUID tenantId) {
        return registryRepository.findByTenantId(tenantId);
    }

    @Transactional
    public TrustRegistry deactivate(UUID tenantId, UUID id) {
        TrustRegistry registry = get(tenantId, id);
        registry.deactivate();
        audit(tenantId, "TRUST_REGISTRY_DEACTIVATED", "TRUST_REGISTRY", registry.getId());
        return registry;
    }

    @Transactional
    public TrustRegistry activate(UUID tenantId, UUID id) {
        TrustRegistry registry = get(tenantId, id);
        registry.activate();
        audit(tenantId, "TRUST_REGISTRY_ACTIVATED", "TRUST_REGISTRY", registry.getId());
        return registry;
    }

    @Transactional
    public TrustedIssuer registerIssuer(UUID tenantId, UUID registryId, String issuerDid, String organization,
                                         List<String> trustedCredentialTypes) {
        TrustRegistry registry = get(tenantId, registryId);
        if (registry.getStatus() != RegistryStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only an ACTIVE trust registry can register issuers: " + registryId);
        }
        boolean alreadyRegistered = issuerRepository.findByTenantIdAndTrustRegistryId(tenantId, registryId).stream()
                .anyMatch(existing -> existing.getIssuerDid().equals(issuerDid));
        if (alreadyRegistered) {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT,
                    "This issuer is already registered in this trust registry: " + issuerDid);
        }

        TrustedIssuer issuer = new TrustedIssuer(tenantId, registryId, issuerDid, organization, trustedCredentialTypes);
        issuerRepository.save(issuer);
        audit(tenantId, "TRUSTED_ISSUER_REGISTERED", "TRUSTED_ISSUER", issuer.getId());
        return issuer;
    }

    public List<TrustedIssuer> listIssuers(UUID tenantId, UUID registryId) {
        get(tenantId, registryId);
        return issuerRepository.findByTenantIdAndTrustRegistryId(tenantId, registryId);
    }

    public TrustedIssuer getIssuer(UUID tenantId, UUID registryId, UUID issuerId) {
        get(tenantId, registryId);
        TrustedIssuer issuer = issuerRepository.findByTenantIdAndId(tenantId, issuerId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Trusted issuer not found: " + issuerId));
        if (!issuer.getTrustRegistryId().equals(registryId)) {
            throw new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Trusted issuer not found in registry: " + issuerId);
        }
        return issuer;
    }

    /** FR-TRUST-004 - only toggles between {@code TRUSTED} and {@code SUSPENDED}; see {@link #revokeTrust} for the terminal transition. */
    @Transactional
    public TrustedIssuer updateStatus(UUID tenantId, UUID registryId, UUID issuerId, TrustStatus status) {
        TrustedIssuer issuer = getIssuer(tenantId, registryId, issuerId);
        if (issuer.getStatus() == TrustStatus.REVOKED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "A revoked trusted issuer cannot change status; re-register instead: " + issuerId);
        }
        if (status != TrustStatus.TRUSTED && status != TrustStatus.SUSPENDED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Use the revoke operation to move a trusted issuer to REVOKED: " + issuerId);
        }
        issuer.updateStatus(status);
        audit(tenantId, "TRUSTED_ISSUER_STATUS_UPDATED", "TRUSTED_ISSUER", issuer.getId());
        return issuer;
    }

    @Transactional
    public TrustedIssuer revokeTrust(UUID tenantId, UUID registryId, UUID issuerId) {
        TrustedIssuer issuer = getIssuer(tenantId, registryId, issuerId);
        issuer.revoke();
        audit(tenantId, "TRUSTED_ISSUER_REVOKED", "TRUSTED_ISSUER", issuer.getId());
        return issuer;
    }

    /** FR-TRUST-006 - every entry across every one of this tenant's registries for the given issuer DID. */
    public List<TrustedIssuer> lookup(UUID tenantId, String issuerDid) {
        return issuerRepository.findByTenantIdAndIssuerDid(tenantId, issuerDid);
    }

    /**
     * FR-TRUST-007 - the dynamic trust check {@code verification.VerificationService} runs
     * alongside a policy's static {@code trustedIssuerDids} allowlist. Trusted means: at least one
     * {@link TrustedIssuer} entry for this DID, in one of this tenant's {@code ACTIVE} registries,
     * with status {@code TRUSTED}, whose {@code trustedCredentialTypes} either is empty (trusted
     * for every type) or names the presented credential's type.
     */
    public boolean isTrusted(UUID tenantId, String issuerDid, String credentialType) {
        Set<UUID> activeRegistryIds = registryRepository.findByTenantId(tenantId).stream()
                .filter(registry -> registry.getStatus() == RegistryStatus.ACTIVE)
                .map(TrustRegistry::getId)
                .collect(Collectors.toSet());
        return issuerRepository.findByTenantIdAndIssuerDid(tenantId, issuerDid).stream()
                .anyMatch(issuer -> issuer.getStatus() == TrustStatus.TRUSTED
                        && activeRegistryIds.contains(issuer.getTrustRegistryId())
                        && (issuer.getTrustedCredentialTypes() == null || issuer.getTrustedCredentialTypes().isEmpty()
                            || credentialType == null || issuer.getTrustedCredentialTypes().contains(credentialType)));
    }

    /**
     * Whether this tenant has registered any trusted issuer at all, in any registry. Used by
     * {@code VerificationService} to decide whether the dynamic Trust Registry check in
     * {@link #isTrusted} should be enforced or skipped for a tenant that hasn't adopted the Trust
     * Registry yet - see that class's Javadoc for the documented transition-period rationale.
     */
    public boolean hasAnyEntries(UUID tenantId) {
        return issuerRepository.existsByTenantId(tenantId);
    }

    private void audit(UUID tenantId, String action, String resourceType, UUID resourceId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                resourceType,
                resourceId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
