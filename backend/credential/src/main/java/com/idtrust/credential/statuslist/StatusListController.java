package com.idtrust.credential.statuslist;

import com.idtrust.common.tenant.TenantContext;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Tenant-scoped read access to a status list's current bitstring. Note this is intentionally NOT
 * the unauthenticated, publicly resolvable endpoint the real StatusList2021 spec requires for
 * verifier consumption (every other resource in this codebase is tenant-scoped behind auth, and
 * introducing the first unauthenticated resource is out of scope for this pass) - see
 * {@code V37__status_lists_core.sql}.
 */
@RestController
@RequestMapping("/api/v1/status-lists")
public class StatusListController {

    private final StatusListService statusListService;

    public StatusListController(StatusListService statusListService) {
        this.statusListService = statusListService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN', 'ISSUER', 'VERIFIER')")
    public StatusListDtos.Response getByIssuer(@RequestParam String issuerDid,
                                                  @RequestParam(defaultValue = "REVOCATION") StatusListPurpose purpose) {
        UUID tenantId = TenantContext.requireTenantId();
        return StatusListDtos.Response.from(statusListService.getByIssuer(tenantId, issuerDid, purpose));
    }
}
