package com.idtrust.wallet.oid4vci;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class Oid4vciDtos {

    private Oid4vciDtos() {
    }

    public record CreateOfferRequest(@NotNull UUID credentialId) {
    }

    public record OfferResponse(String preAuthorizedCode, String credentialOfferUri, String qrCodePngBase64,
                                 Instant expiresAt) {
        public static OfferResponse from(Oid4vciSessionService.OfferResult result) {
            return new OfferResponse(
                    result.session().getPreAuthorizedCode(),
                    result.offer().uri(),
                    result.offer().qrCodePngBase64(),
                    result.session().getExpiresAt()
            );
        }
    }

    /**
     * Token endpoint request (FR-OID4VCI-009). Field names are dictated by the OID4VCI/OAuth2
     * pre-authorized-code grant spec (snake_case, hyphenated {@code pre-authorized_code}), not this
     * codebase's usual camelCase convention - the wallet client sends exactly these wire names.
     */
    public record TokenRequest(@NotBlank String grant_type,
                                @NotBlank @JsonProperty("pre-authorized_code") String preAuthorizedCode) {
    }

    public record TokenResponse(String access_token, String token_type, long expires_in) {
        public static TokenResponse from(Oid4vciSessionService.TokenResult result) {
            return new TokenResponse(result.accessToken(), "bearer", result.expiresInSeconds());
        }
    }

    public record CredentialResponse(String format, String credential) {
        public static CredentialResponse from(Oid4vciSessionService.CredentialResult result) {
            return new CredentialResponse(result.credential().getFormat().name(), result.credential().getCredentialJwt());
        }
    }
}
