package com.idtrust.credential.statuslist;

import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A StatusList2021-shaped compressed bitstring (see {@link BitstringCodec}) for one
 * (tenant, issuer_did, purpose) triple. One bit per credential; {@link #allocateIndex()} hands out
 * the next free bit to a newly issued credential, {@link #setBit} flips it (e.g. on revocation).
 */
@Entity
@Table(name = "status_lists")
public class StatusList {

    @Id
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "issuer_did", nullable = false)
    private String issuerDid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusListPurpose purpose;

    @Column(nullable = false)
    private int size;

    @Column(name = "next_free_index", nullable = false)
    private int nextFreeIndex;

    @Column(name = "encoded_list", nullable = false)
    private String encodedList;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StatusList() {
        // JPA
    }

    /** Per the StatusList2021 spec's recommended minimum size, to make individual entries unlinkable. */
    public static final int DEFAULT_SIZE = 131_072;

    public StatusList(UUID tenantId, String issuerDid, StatusListPurpose purpose, int size) {
        this.id = UUID.randomUUID();
        this.tenantId = tenantId;
        this.issuerDid = issuerDid;
        this.purpose = purpose;
        this.size = size;
        this.nextFreeIndex = 0;
        this.encodedList = BitstringCodec.empty(size);
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public int allocateIndex() {
        if (nextFreeIndex >= size) {
            throw new ApiException(ApiErrorCode.INVALID_REQUEST,
                    "Status list " + id + " is exhausted (size=" + size + ")");
        }
        int index = nextFreeIndex;
        nextFreeIndex++;
        updatedAt = Instant.now();
        return index;
    }

    public void setBit(int index, boolean value) {
        this.encodedList = BitstringCodec.setBit(encodedList, index, value);
        this.updatedAt = Instant.now();
    }

    public boolean getBit(int index) {
        return BitstringCodec.getBit(encodedList, index);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public String getIssuerDid() {
        return issuerDid;
    }

    public StatusListPurpose getPurpose() {
        return purpose;
    }

    public int getSize() {
        return size;
    }

    public int getNextFreeIndex() {
        return nextFreeIndex;
    }

    public String getEncodedList() {
        return encodedList;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
