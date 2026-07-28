package com.idtrust.identity.organization;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

/**
 * Platform-level entity, not tenant-scoped itself (an organization owns one or more tenants).
 * Corresponds to FR-ORG-001/002/003 in the requirements catalogue.
 */
@Entity
@Table(name = "organizations")
public class Organization {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrganizationStatus status;

    @Column(name = "logo_url")
    private String logoUrl;

    private String website;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected Organization() {
        // JPA
    }

    public Organization(String name, String description, String contactEmail) {
        this.id = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.contactEmail = contactEmail;
        this.status = OrganizationStatus.ACTIVE;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public void updateProfile(String name, String description, String logoUrl, String website, String contactEmail) {
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (logoUrl != null) {
            this.logoUrl = logoUrl;
        }
        if (website != null) {
            this.website = website;
        }
        if (contactEmail != null) {
            this.contactEmail = contactEmail;
        }
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = OrganizationStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public OrganizationStatus getStatus() {
        return status;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public String getWebsite() {
        return website;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getLockVersion() {
        return lockVersion;
    }
}
