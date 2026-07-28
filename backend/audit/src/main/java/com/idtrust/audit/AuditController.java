package com.idtrust.audit;

import com.idtrust.common.tenant.TenantContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * FR-AUD-009 (Search Audit) and FR-AUD-010 (Export Audit) - a read-only query surface over the
 * tenant's own audit trail. Restricted to PLATFORM_ADMIN/ORG_ADMIN, mirroring
 * {@code apiplatform.log.ApiRequestLogController}'s exact precedent: an audit trail is
 * operational/compliance evidence, not data any non-admin role needs to browse.
 */
@RestController
@RequestMapping("/api/v1/audit-records")
@PreAuthorize("hasAnyRole('PLATFORM_ADMIN', 'ORG_ADMIN')")
public class AuditController {

    private final AuditRecordRepository auditRecordRepository;

    public AuditController(AuditRecordRepository auditRecordRepository) {
        this.auditRecordRepository = auditRecordRepository;
    }

    @GetMapping
    public List<AuditRecordDtos.AuditRecordResponse> search(@RequestParam(required = false) String action,
                                                              @RequestParam(required = false) String resourceType,
                                                              @RequestParam(required = false) UUID actorUserId,
                                                              @RequestParam(required = false) Instant from,
                                                              @RequestParam(required = false) Instant to,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "50") int size) {
        return doSearch(action, resourceType, actorUserId, from, to, page, size)
                .map(AuditRecordDtos.AuditRecordResponse::from)
                .toList();
    }

    /**
     * FR-AUD-010 (Export Audit) - a CSV dump of the same filtered result set as {@link #search},
     * capped at 5,000 rows per request (no background export job/object storage exists anywhere
     * in this codebase to hand back a downloadable link for a larger one - a documented
     * limitation, see the root README's E15 section).
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String action,
                                          @RequestParam(required = false) String resourceType,
                                          @RequestParam(required = false) UUID actorUserId,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to) {
        List<AuditRecord> rows = doSearch(action, resourceType, actorUserId, from, to, 0, 5000).toList();

        StringBuilder csv = new StringBuilder("id,tenantId,actorUserId,action,resourceType,resourceId,result,occurredAt\n");
        for (AuditRecord row : rows) {
            csv.append(csvField(row.getId())).append(',')
                    .append(csvField(row.getTenantId())).append(',')
                    .append(csvField(row.getActorUserId())).append(',')
                    .append(csvField(row.getAction())).append(',')
                    .append(csvField(row.getResourceType())).append(',')
                    .append(csvField(row.getResourceId())).append(',')
                    .append(csvField(row.getResult())).append(',')
                    .append(csvField(row.getOccurredAt())).append('\n');
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDisposition(ContentDisposition.attachment().filename("audit-export.csv").build());
        return ResponseEntity.ok().headers(headers).body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private java.util.stream.Stream<AuditRecord> doSearch(String action, String resourceType, UUID actorUserId,
                                                            Instant from, Instant to, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, Math.min(Math.max(size, 1), 5000), Sort.by("occurredAt").descending());
        return auditRecordRepository.search(TenantContext.requireTenantId(), action, resourceType, actorUserId,
                from, to, pageRequest).stream();
    }

    private String csvField(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString().replace("\"", "\"\"");
        return "\"" + text + "\"";
    }
}
