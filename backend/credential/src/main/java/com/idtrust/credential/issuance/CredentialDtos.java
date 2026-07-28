package com.idtrust.credential.issuance;

import com.idtrust.credential.schema.CredentialFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public final class CredentialDtos {

    private CredentialDtos() {
    }

    public record IssueRequest(
            @NotNull UUID schemaId,
            @NotNull String issuerDid,
            String subjectDid,
            @NotNull @NotEmpty Map<String, Object> claims,
            Instant expiresAt
    ) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            UUID schemaId,
            String issuerDid,
            String subjectDid,
            CredentialFormat format,
            String credentialJwt,
            String credentialHash,
            Map<String, Object> claims,
            CredentialStatus status,
            String statusReason,
            Instant issuedAt,
            Instant expiresAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Credential credential) {
            return new Response(
                    credential.getId(),
                    credential.getTenantId(),
                    credential.getSchemaId(),
                    credential.getIssuerDid(),
                    credential.getSubjectDid(),
                    credential.getFormat(),
                    credential.getCredentialJwt(),
                    credential.getCredentialHash(),
                    credential.getClaims(),
                    credential.getStatus(),
                    credential.getStatusReason(),
                    credential.getIssuedAt(),
                    credential.getExpiresAt(),
                    credential.getCreatedAt(),
                    credential.getUpdatedAt()
            );
        }
    }

    public record IssueResponse(Response credential, String credentialOfferUri, String credentialOfferQrCodePng) {
        public static IssueResponse from(CredentialIssuanceService.IssuedCredential issued) {
            return new IssueResponse(
                    Response.from(issued.credential()),
                    issued.offer().uri(),
                    issued.offer().qrCodePngBase64()
            );
        }
    }
}
