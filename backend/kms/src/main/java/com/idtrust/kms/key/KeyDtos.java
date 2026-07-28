package com.idtrust.kms.key;

import com.idtrust.kms.crypto.KeyAlgorithm;
import com.idtrust.kms.provider.KeyProviderType;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class KeyDtos {

    private KeyDtos() {
    }

    public record CreateRequest(@NotNull KeyType keyType, @NotNull KeyAlgorithm algorithm, KeyProviderType provider) {
    }

    /**
     * Never includes private key material - signing happens in-process via {@link KeyService#sign},
     * not through this API (see {@link com.idtrust.kms.provider.KeyProvider}).
     */
    public record Response(
            UUID id,
            UUID tenantId,
            UUID didId,
            KeyType keyType,
            KeyAlgorithm algorithm,
            KeyProviderType provider,
            String publicKey,
            UUID rotatedFromKeyId,
            KeyStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(ManagedKey key) {
            return new Response(
                    key.getId(),
                    key.getTenantId(),
                    key.getDidId(),
                    key.getKeyType(),
                    key.getAlgorithm(),
                    key.getProvider(),
                    key.getPublicKey(),
                    key.getRotatedFromKeyId(),
                    key.getStatus(),
                    key.getCreatedAt(),
                    key.getUpdatedAt()
            );
        }
    }
}
