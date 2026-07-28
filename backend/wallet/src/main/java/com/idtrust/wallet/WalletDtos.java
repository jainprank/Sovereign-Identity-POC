package com.idtrust.wallet;

import com.idtrust.credential.issuance.CredentialDtos;
import com.idtrust.wallet.oid4vp.WalletPresentation;
import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.List;
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

    /**
     * {@code disclosedClaims} is only meaningful for an SD-JWT VC credential (FR-WAL-014 -
     * Selective Disclosure): {@code null}/empty discloses every claim (the only option for a
     * plain W3C JWT-VC, which has no disclosures to withhold), a non-empty list reveals only the
     * named claims. See {@code WalletService#presentCredential}.
     */
    public record PresentRequest(@NotBlank String audience, @NotBlank String nonce, List<String> disclosedClaims) {
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

    public record PresentationResponse(UUID id, UUID walletId, UUID walletCredentialId, String audience,
                                        String vpJwt, Instant presentedAt) {
        public static PresentationResponse from(WalletPresentation presentation) {
            return new PresentationResponse(
                    presentation.getId(),
                    presentation.getWalletId(),
                    presentation.getWalletCredentialId(),
                    presentation.getAudience(),
                    presentation.getPresentationJwt(),
                    presentation.getPresentedAt()
            );
        }
    }
}
