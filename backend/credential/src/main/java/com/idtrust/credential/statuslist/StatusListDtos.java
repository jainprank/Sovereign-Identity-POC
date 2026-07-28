package com.idtrust.credential.statuslist;

import java.time.Instant;
import java.util.UUID;

public final class StatusListDtos {

    private StatusListDtos() {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            String issuerDid,
            StatusListPurpose purpose,
            int size,
            int nextFreeIndex,
            String encodedList,
            Instant updatedAt
    ) {
        public static Response from(StatusList statusList) {
            return new Response(
                    statusList.getId(),
                    statusList.getTenantId(),
                    statusList.getIssuerDid(),
                    statusList.getPurpose(),
                    statusList.getSize(),
                    statusList.getNextFreeIndex(),
                    statusList.getEncodedList(),
                    statusList.getUpdatedAt()
            );
        }
    }
}
