package com.idtrust.identity.invitation;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.identity.rbac.RoleService;
import com.idtrust.identity.user.AppUser;
import com.idtrust.identity.user.UserRepository;
import com.idtrust.identity.user.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements FR-ORG-007 (Invite User) and FR-ORG-008 (Accept Invitation). Invitations default to
 * a 7-day expiry (Master Specification, section 6 "User Personas / Onboarding").
 */
@Service
public class InvitationService {

    private static final Duration DEFAULT_EXPIRY = Duration.ofDays(7);
    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    private final InvitationRepository invitationRepository;
    private final RoleService roleService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final AuditEventPublisher auditEventPublisher;

    public InvitationService(InvitationRepository invitationRepository, RoleService roleService,
                              UserRepository userRepository, UserService userService,
                              AuditEventPublisher auditEventPublisher) {
        this.invitationRepository = invitationRepository;
        this.roleService = roleService;
        this.userRepository = userRepository;
        this.userService = userService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public Invitation create(UUID tenantId, String email, UUID roleId) {
        roleService.get(tenantId, roleId);
        Invitation invitation = new Invitation(tenantId, email, roleId, generateToken(), Instant.now().plus(DEFAULT_EXPIRY));
        invitationRepository.save(invitation);
        audit(tenantId, "INVITATION_CREATED", invitation.getId());
        return invitation;
    }

    public List<Invitation> listByTenant(UUID tenantId) {
        return invitationRepository.findByTenantId(tenantId);
    }

    public Invitation get(UUID tenantId, UUID id) {
        return invitationRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Invitation not found: " + id));
    }

    @Transactional
    public Invitation revoke(UUID tenantId, UUID id) {
        Invitation invitation = get(tenantId, id);
        if (!invitation.isPending()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only a pending invitation can be revoked");
        }
        invitation.revoke();
        audit(tenantId, "INVITATION_REVOKED", invitation.getId());
        return invitation;
    }

    /**
     * Accepts an invitation by token: not tenant-scoped by header, since the caller is not yet an
     * authenticated tenant member - the tenant is resolved from the invitation record itself.
     */
    @Transactional
    public AppUser accept(String token, String displayName, String keycloakSubjectId) {
        Invitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Invitation not found"));
        if (!invitation.isPending() || invitation.isExpired()) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Invitation is no longer valid");
        }
        AppUser user = userRepository.findByTenantIdAndEmail(invitation.getTenantId(), invitation.getEmail())
                .orElseGet(() -> userService.create(invitation.getTenantId(), invitation.getEmail(), displayName));
        if (keycloakSubjectId != null && !keycloakSubjectId.isBlank()) {
            user.linkKeycloakSubject(keycloakSubjectId);
        }
        roleService.assign(invitation.getTenantId(), user.getId(), invitation.getRoleId());
        invitation.accept();
        audit(invitation.getTenantId(), "INVITATION_ACCEPTED", invitation.getId());
        return user;
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        TOKEN_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void audit(UUID tenantId, String action, UUID invitationId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "INVITATION",
                invitationId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
