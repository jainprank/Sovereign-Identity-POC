package com.idtrust.hedera;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only API over the global {@code hedera_networks} reference catalogue - not tenant-scoped
 * (see {@link HederaNetwork}'s Javadoc), so every authenticated admin/verifier role may read it.
 */
@RestController
@RequestMapping("/api/v1/hedera/networks")
public class HederaNetworkController {

    private final HederaNetworkRepository networkRepository;

    public HederaNetworkController(HederaNetworkRepository networkRepository) {
        this.networkRepository = networkRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public List<HederaDtos.NetworkResponse> list() {
        return networkRepository.findAllByOrderByNetworkName().stream()
                .map(HederaDtos.NetworkResponse::from).toList();
    }

    @GetMapping("/{networkName}")
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public HederaDtos.NetworkResponse get(@PathVariable String networkName) {
        return networkRepository.findByNetworkName(networkName)
                .map(HederaDtos.NetworkResponse::from)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Hedera network not found: " + networkName));
    }
}
