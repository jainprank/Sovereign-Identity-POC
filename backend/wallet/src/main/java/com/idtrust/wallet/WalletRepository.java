package com.idtrust.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository extends JpaRepository<Wallet, UUID> {

    Optional<Wallet> findByTenantIdAndId(UUID tenantId, UUID id);

    List<Wallet> findByTenantId(UUID tenantId);

    List<Wallet> findByTenantIdAndOwnerUserId(UUID tenantId, UUID ownerUserId);
}
