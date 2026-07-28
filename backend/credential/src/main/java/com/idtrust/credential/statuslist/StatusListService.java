package com.idtrust.credential.statuslist;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Allocates and mutates the StatusList2021-shaped bitstrings backing FR-CRED-012. One list per
 * (tenant, issuer_did, purpose), created lazily on first allocation.
 */
@Service
public class StatusListService {

    private final StatusListRepository statusListRepository;

    public StatusListService(StatusListRepository statusListRepository) {
        this.statusListRepository = statusListRepository;
    }

    /** Allocates the next free bit index for a newly issued credential, creating the list if needed. */
    @Transactional
    public Allocation allocate(UUID tenantId, String issuerDid, StatusListPurpose purpose) {
        StatusList statusList = statusListRepository.findByTenantIdAndIssuerDidAndPurpose(tenantId, issuerDid, purpose)
                .orElseGet(() -> statusListRepository.save(
                        new StatusList(tenantId, issuerDid, purpose, StatusList.DEFAULT_SIZE)));
        int index = statusList.allocateIndex();
        return new Allocation(statusList.getId(), index);
    }

    public StatusList get(UUID tenantId, UUID id) {
        return statusListRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Status list not found: " + id));
    }

    public StatusList getByIssuer(UUID tenantId, String issuerDid, StatusListPurpose purpose) {
        return statusListRepository.findByTenantIdAndIssuerDidAndPurpose(tenantId, issuerDid, purpose)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "No status list found for issuer " + issuerDid + " / " + purpose));
    }

    /** Flips the bit for a credential's assigned status-list index, e.g. on revocation. */
    @Transactional
    public void setBit(UUID tenantId, UUID statusListId, int index, boolean value) {
        StatusList statusList = get(tenantId, statusListId);
        statusList.setBit(index, value);
    }

    public record Allocation(UUID statusListId, int index) {
    }
}
