package com.idtrust.wallet;

import com.idtrust.credential.issuance.CredentialDtos;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class WalletDtos {

    private WalletDtos() {
    }

    public record CreateRequest(@NotBlank String label) {
    }

    public record ClaimRequest(@NotBlank String preAuthorizedCode) {
    }

    public record RecategorizeRequest(String category) {
    }

    public record Response(
            UUID id,
            UUID tenantId,
            UUID ownerUserId,
            String did,
            String label,
            WalletStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        public static Response from(Wallet wallet) {
            return new Response(
                    wallet.getId(),
                    wallet.getTenantId(),
                    wallet.getOwnerUserId(),
                    wallet.getDid(),
                    wallet.getLabel(),
                    wallet.getStatus(),
                    wallet.getCreatedAt(),
                    wallet.getUpdatedAt()
            );
        }
    }

    public record WalletCredentialResponse(
            UUID id,
            UUID walletId,
            String category,
            Instant receivedAt,
            CredentialDtos.Response credential
    ) {
        public static WalletCredentialResponse from(WalletService.WalletCredentialView view) {
            return new WalletCredentialResponse(
                    view.walletCredential().getId(),
                    view.walletCredential().getWalletId(),
                    view.walletCredential().getCategory(),
                    view.walletCredential().getReceivedAt(),
                    CredentialDtos.Response.from(view.credential())
            );
        }
    }
}
