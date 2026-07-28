package com.idtrust.apiplatform.client;

/**
 * Which FR-API-* credential type an {@link ApiClient} row represents. Both share one table (the
 * Baseline's ERD shows a single {@code API_CLIENT} node under {@code ORGANIZATION}, not separate
 * API-key/OAuth-client entities) since both are, at their core, "a named machine identity
 * belonging to a tenant with a set of scopes" - only how the credential is presented differs.
 */
public enum ApiClientType {

    /** FR-API-001 - the client authenticates by presenting {@code secretHash}'s plaintext value directly as a bearer token. */
    API_KEY,

    /** FR-API-002 - the client authenticates via {@code POST /api/v1/oauth/token} (client_credentials grant) using {@code clientId}/the secret, then presents the minted {@code ApiAccessToken} as a bearer token. */
    OAUTH_CLIENT
}
