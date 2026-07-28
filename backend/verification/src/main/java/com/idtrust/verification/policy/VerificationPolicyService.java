package com.idtrust.verification.policy;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 12 (Policy Engine): FR-POL-001 (Create Policy), FR-POL-002 (View Policy),
 * FR-POL-003 (Update Policy), FR-POL-004 (Activate Policy), FR-POL-005 (Deactivate Policy),
 * FR-POL-006 through 011 (rule definition, carried entirely by {@link PolicyDefinition}).
 * FR-POL-012 through 015 (policy templates, priority ordering, testing sandbox, versioning
 * analytics) are V1-priority and out of scope for this pass - mirrors E06's precedent of skipping
 * FR-SCH-012/013.
 */
@Service
public class VerificationPolicyService {

    private final VerificationPolicyRepository policyRepository;
    private final AuditEventPublisher auditEventPublisher;

    public VerificationPolicyService(VerificationPolicyRepository policyRepository,
                                      AuditEventPublisher auditEventPublisher) {
        this.policyRepository = policyRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public VerificationPolicy create(UUID tenantId, String name, PolicyDefinition definition) {
        requireUniqueVersion(tenantId, name, 1);
        VerificationPolicy policy = new VerificationPolicy(tenantId, name, 1, definition);
        policyRepository.save(policy);
        audit(tenantId, "VERIFICATION_POLICY_CREATED", policy.getId());
        return policy;
    }

    public VerificationPolicy get(UUID tenantId, UUID id) {
        return policyRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Verification policy not found: " + id));
    }

    public List<VerificationPolicy> search(UUID tenantId, String name, PolicyStatus status) {
        return policyRepository.search(tenantId, name, status);
    }

    @Transactional
    public VerificationPolicy update(UUID tenantId, UUID id, PolicyDefinition definition) {
        VerificationPolicy policy = get(tenantId, id);
        requireDraft(policy);
        policy.replaceDefinition(definition);
        audit(tenantId, "VERIFICATION_POLICY_UPDATED", policy.getId());
        return policy;
    }

    @Transactional
    public VerificationPolicy activate(UUID tenantId, UUID id) {
        VerificationPolicy policy = get(tenantId, id);
        requireDraft(policy);
        policy.activate();
        audit(tenantId, "VERIFICATION_POLICY_ACTIVATED", policy.getId());
        return policy;
    }

    @Transactional
    public VerificationPolicy deactivate(UUID tenantId, UUID id) {
        VerificationPolicy policy = get(tenantId, id);
        if (policy.getStatus() != PolicyStatus.ACTIVE) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only an ACTIVE policy can be deactivated: " + id);
        }
        policy.deactivate();
        audit(tenantId, "VERIFICATION_POLICY_DEACTIVATED", policy.getId());
        return policy;
    }

    @Transactional
    public VerificationPolicy createNewVersion(UUID tenantId, UUID id, PolicyDefinition definition) {
        VerificationPolicy previous = get(tenantId, id);
        int nextVersion = previous.getVersion() + 1;
        requireUniqueVersion(tenantId, previous.getName(), nextVersion);
        PolicyDefinition nextDefinition = definition != null ? definition : previous.getDefinition();
        VerificationPolicy next = new VerificationPolicy(tenantId, previous.getName(), nextVersion, nextDefinition);
        policyRepository.save(next);
        audit(tenantId, "VERIFICATION_POLICY_VERSIONED", next.getId());
        return next;
    }

    private void requireUniqueVersion(UUID tenantId, String name, int version) {
        policyRepository.findByTenantIdAndNameAndVersion(tenantId, name, version).ifPresent(existing -> {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT,
                    "A policy named '" + name + "' at version " + version + " already exists");
        });
    }

    private void requireDraft(VerificationPolicy policy) {
        if (policy.getStatus() != PolicyStatus.DRAFT) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only a DRAFT policy can be modified or activated; create a new version instead: " + policy.getId());
        }
    }

    private void audit(UUID tenantId, String action, UUID policyId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "VERIFICATION_POLICY",
                policyId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
