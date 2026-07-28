package com.idtrust.identity.rbac;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * A global, platform-wide permission (resource + action pair). The catalogue is seeded via SQL
 * migration (see V2__identity_core.sql); permissions are not created through the API. Corresponds
 * to the "resource/action" model in the Architecture &amp; Implementation Baseline, section 8
 * ("RBAC Model").
 */
@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String resource;

    @Column(nullable = false)
    private String action;

    private String description;

    protected Permission() {
        // JPA
    }

    public UUID getId() {
        return id;
    }

    public String getResource() {
        return resource;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }
}
