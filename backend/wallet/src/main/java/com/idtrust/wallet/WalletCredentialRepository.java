package com.idtrust.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WalletCredentialRepository extends JpaRepository<WalletCredential, UUID> {

    Optional<WalletCredential> findByTenantIdAndWalletIdAndId(UUID tenantId, UUID walletId, UUID id);

    Optional<WalletCredential> findByTenantIdAndWalletIdAndCredentialId(UUID tenantId, UUID walletId, UUID credentialId);

    @Query("""
            SELECT wc FROM WalletCredential wc
            WHERE wc.tenantId = :tenantId AND wc.walletId = :walletId
            AND (:category IS NULL OR wc.category = :category)
            ORDER BY wc.receivedAt DESC
            """)
    List<WalletCredential> search(@Param("tenantId") UUID tenantId, @Param("walletId") UUID walletId,
                                   @Param("category") String category);
}
