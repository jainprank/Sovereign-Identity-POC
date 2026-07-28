package com.idtrust.apiplatform.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiAccessTokenRepository extends JpaRepository<ApiAccessToken, UUID> {

    Optional<ApiAccessToken> findByTokenHash(String tokenHash);
}
