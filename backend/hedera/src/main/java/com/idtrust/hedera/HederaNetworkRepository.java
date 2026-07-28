package com.idtrust.hedera;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HederaNetworkRepository extends JpaRepository<HederaNetwork, UUID> {

    List<HederaNetwork> findAllByOrderByNetworkName();

    Optional<HederaNetwork> findByNetworkName(String networkName);
}
