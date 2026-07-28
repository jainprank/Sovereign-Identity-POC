package com.idtrust.apiplatform.auth;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * FR-API-002 (OAuth Client) - the {@code client_credentials} grant token endpoint. Permitted
 * without authentication in {@code identity.security.SecurityConfig} (the client authenticates via
 * its own {@code clientId}/{@code clientSecret} in the request body, exactly like every standard
 * OAuth2 token endpoint).
 */
@RestController
@RequestMapping("/api/v1/oauth")
public class OAuthTokenController {

    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";

    private final OAuthTokenService oAuthTokenService;

    public OAuthTokenController(OAuthTokenService oAuthTokenService) {
        this.oAuthTokenService = oAuthTokenService;
    }

    @PostMapping("/token")
    public OAuthDtos.TokenResponse token(@Valid @RequestBody OAuthDtos.TokenRequest request) {
        if (!GRANT_TYPE_CLIENT_CREDENTIALS.equals(request.grantType())) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only the client_credentials grant type is supported");
        }
        return OAuthDtos.TokenResponse.from(oAuthTokenService.mint(request.clientId(), request.clientSecret()));
    }
}
