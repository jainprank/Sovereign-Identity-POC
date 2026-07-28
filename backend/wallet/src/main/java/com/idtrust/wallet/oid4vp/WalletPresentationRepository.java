package com.idtrust.wallet.oid4vp;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WalletPresentationRepository extends JpaRepository<WalletPresentation, UUID> {

    List<WalletPresentation> findByTenantIdAndWalletId(UUID tenantId, UUID walletId);
}
