package com.idtrust.apiplatform.auth;

import jakarta.validation.constraints.NotBlank;

public final class OAuthDtos {

    private OAuthDtos() {
    }

    /**
     * Deliberately not tied to OAuth2's {@code application/x-www-form-urlencoded} convention for
     * {@code grant_type}/{@code client_id}/{@code client_secret} - every other endpoint in this
     * codebase accepts JSON, so this token endpoint does too, for consistency with the rest of the
     * platform's API contract standard (Baseline section 9).
     */
    public record TokenRequest(
            @NotBlank String grantType,
            @NotBlank String clientId,
            @NotBlank String clientSecret
    ) {
    }

    public record TokenResponse(String accessToken, String tokenType, long expiresIn, String scope) {
        public static TokenResponse from(OAuthTokenService.MintedToken minted) {
            return new TokenResponse(minted.accessToken(), "Bearer", minted.expiresInSeconds(), minted.scope());
        }
    }
}
