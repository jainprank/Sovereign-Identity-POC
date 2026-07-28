package com.idtrust.apiplatform.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FR-API-006 (OpenAPI) - genuine generated API documentation over every {@code @RestController}
 * across every module (springdoc introspects the whole application context; no per-controller
 * annotation is required beyond what already exists), exposed at {@code /v3/api-docs} and
 * {@code /swagger-ui.html}. Declares the platform's two bearer-token authentication schemes
 * (Keycloak JWT, and the FR-API-001/002 API-key/OAuth-access-token path - both are presented
 * identically as {@code Authorization: Bearer <token>}) so "Try it out" works from the UI.
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI apiPlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("IDTrust API")
                        .description("Identity, credential and trust platform API - see the root README's "
                                + "epic-by-epic build history for scope and known limitations.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));
    }
}
