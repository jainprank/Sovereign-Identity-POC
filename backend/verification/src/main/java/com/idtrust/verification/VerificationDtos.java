package com.idtrust.verification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class VerificationDtos {

    private VerificationDtos() {
    }

    public record CreateRequest(@NotNull UUID policyId, String callbackUrl) {
    }

    public record PresentRequest(@NotBlank String vpJwt) {
    }

    public record RequestResponse(UUID id, UUID tenantId, UUID policyId, String nonce, String oid4vpRequestUri,
                                   String qrCodePngBase64, VerificationStatus status, Instant createdAt,
                                   Instant expiresAt) {
        public static RequestResponse from(VerificationService.RequestResult result) {
            VerificationRequest request = result.request();
            return new RequestResponse(
                    request.getId(),
                    request.getTenantId(),
                    request.getPolicyId(),
                    request.getNonce(),
                    result.oid4vpRequest().uri(),
                    result.oid4vpRequest().qrCodePngBase64(),
                    request.getStatus(),
                    request.getCreatedAt(),
                    request.getExpiresAt()
            );
        }
    }

    public record ResultResponse(UUID id, UUID tenantId, UUID policyId, VerificationStatus status,
                                  String statusReason, Map<String, Object> checks, UUID verifiedCredentialId,
                                  String verifiedSubjectDid, Instant createdAt, Instant expiresAt,
                                  Instant verifiedAt) {
        public static ResultResponse from(VerificationRequest request) {
            return new ResultResponse(
                    request.getId(),
                    request.getTenantId(),
                    request.getPolicyId(),
                    request.getStatus(),
                    request.getStatusReason(),
                    request.getChecks(),
                    request.getVerifiedCredentialId(),
                    request.getVerifiedSubjectDid(),
                    request.getCreatedAt(),
                    request.getExpiresAt(),
                    request.getVerifiedAt()
            );
        }
    }
}
