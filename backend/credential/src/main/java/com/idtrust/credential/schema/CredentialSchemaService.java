package com.idtrust.credential.schema;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 5 (Credential Schemas): FR-SCH-001 (Create), FR-SCH-002 (View), FR-SCH-003
 * (Update), FR-SCH-004 (Version), FR-SCH-005 (Publish), FR-SCH-006 (Deprecate), FR-SCH-014
 * (Search). FR-SCH-012/013 (Import/Export) are V1-priority and out of scope for this pass.
 */
@Service
public class CredentialSchemaService {

    private final CredentialSchemaRepository schemaRepository;
    private final AuditEventPublisher auditEventPublisher;

    public CredentialSchemaService(CredentialSchemaRepository schemaRepository,
                                    AuditEventPublisher auditEventPublisher) {
        this.schemaRepository = schemaRepository;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public CredentialSchema create(UUID tenantId, String name, String version, CredentialFormat format,
                                    SchemaDefinition definition) {
        requireUniqueVersion(tenantId, name, version);
        CredentialSchema schema = new CredentialSchema(tenantId, name, version, format, definition);
        schemaRepository.save(schema);
        audit(tenantId, "SCHEMA_CREATED", schema.getId());
        return schema;
    }

    public CredentialSchema get(UUID tenantId, UUID id) {
        return schemaRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Credential schema not found: " + id));
    }

    public List<CredentialSchema> search(UUID tenantId, String name, SchemaStatus status) {
        return schemaRepository.search(tenantId, name, status);
    }

    @Transactional
    public CredentialSchema update(UUID tenantId, UUID id, String name, SchemaDefinition definition) {
        CredentialSchema schema = get(tenantId, id);
        requireDraft(schema);
        schema.rename(name);
        schema.replaceDefinition(definition);
        audit(tenantId, "SCHEMA_UPDATED", schema.getId());
        return schema;
    }

    @Transactional
    public CredentialSchema publish(UUID tenantId, UUID id) {
        CredentialSchema schema = get(tenantId, id);
        requireDraft(schema);
        schema.publish();
        audit(tenantId, "SCHEMA_PUBLISHED", schema.getId());
        return schema;
    }

    @Transactional
    public CredentialSchema deprecate(UUID tenantId, UUID id) {
        CredentialSchema schema = get(tenantId, id);
        if (schema.getStatus() != SchemaStatus.PUBLISHED) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST, "Only a PUBLISHED schema can be deprecated: " + id);
        }
        schema.deprecate();
        audit(tenantId, "SCHEMA_DEPRECATED", schema.getId());
        return schema;
    }

    @Transactional
    public CredentialSchema createNewVersion(UUID tenantId, UUID id, String newVersion, SchemaDefinition definition) {
        CredentialSchema previous = get(tenantId, id);
        requireUniqueVersion(tenantId, previous.getName(), newVersion);
        SchemaDefinition nextDefinition = definition != null ? definition : previous.getDefinition();
        CredentialSchema next = new CredentialSchema(tenantId, previous.getName(), newVersion, previous.getFormat(), nextDefinition);
        schemaRepository.save(next);
        audit(tenantId, "SCHEMA_VERSIONED", next.getId());
        return next;
    }

    private void requireUniqueVersion(UUID tenantId, String name, String version) {
        schemaRepository.findByTenantIdAndNameAndVersion(tenantId, name, version).ifPresent(existing -> {
            throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT,
                    "A schema named '" + name + "' at version '" + version + "' already exists");
        });
    }

    private void requireDraft(CredentialSchema schema) {
        if (schema.getStatus() != SchemaStatus.DRAFT) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Only a DRAFT schema can be modified or published; create a new version instead: " + schema.getId());
        }
    }

    private void audit(UUID tenantId, String action, UUID schemaId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "CREDENTIAL_SCHEMA",
                schemaId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }
}
