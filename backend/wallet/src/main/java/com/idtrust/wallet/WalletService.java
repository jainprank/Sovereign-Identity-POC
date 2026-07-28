package com.idtrust.wallet;

import com.idtrust.common.audit.AuditEvent;
import com.idtrust.common.audit.AuditEventPublisher;
import com.idtrust.common.tenant.CurrentUserContext;
import com.idtrust.common.web.ApiErrorCode;
import com.idtrust.common.web.ApiException;
import com.idtrust.credential.issuance.Credential;
import com.idtrust.credential.issuance.CredentialIssuanceService;
import com.idtrust.did.Did;
import com.idtrust.did.DidService;
import com.idtrust.did.provider.DidMethod;
import com.idtrust.kms.key.KeyService;
import com.idtrust.kms.key.ManagedKey;
import com.idtrust.wallet.oid4vci.Oid4vciSessionService;
import com.idtrust.wallet.oid4vp.WalletPresentation;
import com.idtrust.wallet.oid4vp.WalletPresentationRepository;
import com.idtrust.wallet.oid4vp.WalletPresentationSigner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Implements EPIC 8 (Digital Wallet): FR-WAL-001 through 009. FR-WAL-002 (Wallet DID) and
 * FR-WAL-003 (Wallet Key) are both satisfied by a single {@link DidService#create} call - that
 * call already provisions a signing key via KMS as a side effect (E04/E05), so no separate
 * key-provisioning step is needed here.
 *
 * <p>FR-WAL-004/005 (Credential Offer Acceptance, Credential Storage) are implemented by
 * {@link #claim}, which drives {@link Oid4vciSessionService#issueToken} and
 * {@link Oid4vciSessionService#redeemCredential} directly (in-process) rather than via an HTTP
 * loopback to this platform's own OID4VCI endpoints - the wallet already trusts this platform's
 * session state, so a network round-trip back into itself would add nothing but latency.
 *
 * <p>FR-WAL-007 (Delete Credential) removes only the {@link WalletCredential} reference row, never
 * the underlying {@code Credential} - deleting a credential from a wallet is a local, holder-side
 * decision and must not affect the issuer's authoritative record (which remains subject to E08's
 * revoke/suspend/renew lifecycle regardless of which wallets still reference it).
 *
 * <p>FR-WAL-011/012/017 (Select Credentials for Presentation, Generate Presentation, Presentation
 * History) are implemented by {@link #presentCredential} now that E10 exists: it signs a VP JWT
 * via {@link WalletPresentationSigner} using the same KMS-backed key {@link #create} already
 * provisioned, and records a {@link WalletPresentation} history row. FR-WAL-013 through 016
 * (multi-credential/derived-claim presentations, presentation preview, revocation-aware
 * presentation) remain out of scope - this pass only ever presents one whole credential per
 * request. Deliberately, {@code wallet} does not gain a Gradle dependency on {@code verification}
 * here (see this module's README section) - the caller is expected to independently submit the
 * returned VP JWT to the verifier's own {@code POST /api/v1/verification-requests/{id}/present}.
 * FR-WAL-018 through 022 (backup/recovery, multi-device sync, lock/PIN, biometric, offline mode)
 * are V1/V2-priority and also out of scope.
 *
 * <p>{@link #list} is a known limitation: it returns every wallet in the tenant rather than only
 * those owned by the calling holder, since no per-holder "my resources" pattern exists anywhere
 * else in this codebase yet either. Documented in the README rather than worked around here.
 */
@Service
public class WalletService {

    private static final Duration PRESENTATION_TTL = Duration.ofMinutes(5);

    private final WalletRepository walletRepository;
    private final WalletCredentialRepository walletCredentialRepository;
    private final WalletPresentationRepository walletPresentationRepository;
    private final DidService didService;
    private final CredentialIssuanceService credentialIssuanceService;
    private final Oid4vciSessionService oid4vciSessionService;
    private final KeyService keyService;
    private final AuditEventPublisher auditEventPublisher;

    public WalletService(WalletRepository walletRepository, WalletCredentialRepository walletCredentialRepository,
                          WalletPresentationRepository walletPresentationRepository, DidService didService,
                          CredentialIssuanceService credentialIssuanceService,
                          Oid4vciSessionService oid4vciSessionService, KeyService keyService,
                          AuditEventPublisher auditEventPublisher) {
        this.walletRepository = walletRepository;
        this.walletCredentialRepository = walletCredentialRepository;
        this.walletPresentationRepository = walletPresentationRepository;
        this.didService = didService;
        this.credentialIssuanceService = credentialIssuanceService;
        this.oid4vciSessionService = oid4vciSessionService;
        this.keyService = keyService;
        this.auditEventPublisher = auditEventPublisher;
    }

    @Transactional
    public Wallet create(UUID tenantId, String label) {
        UUID ownerUserId = CurrentUserContext.getUserId();
        Did did = didService.create(tenantId, DidMethod.KEY, null, null);

        Wallet wallet = new Wallet(tenantId, ownerUserId, did.getId(), did.getDid(), label);
        walletRepository.save(wallet);
        audit(tenantId, "WALLET_CREATED", wallet.getId());
        return wallet;
    }

    public Wallet get(UUID tenantId, UUID id) {
        return walletRepository.findByTenantIdAndId(tenantId, id)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND, "Wallet not found: " + id));
    }

    public List<Wallet> list(UUID tenantId) {
        return walletRepository.findByTenantId(tenantId);
    }

    public List<WalletCredentialView> listCredentials(UUID tenantId, UUID walletId, String category) {
        get(tenantId, walletId);
        return walletCredentialRepository.search(tenantId, walletId, category).stream()
                .map(wc -> new WalletCredentialView(wc, credentialIssuanceService.get(tenantId, wc.getCredentialId())))
                .toList();
    }

    public WalletCredentialView getCredential(UUID tenantId, UUID walletId, UUID walletCredentialId) {
        get(tenantId, walletId);
        WalletCredential wc = walletCredentialRepository.findByTenantIdAndWalletIdAndId(tenantId, walletId, walletCredentialId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found in wallet: " + walletCredentialId));
        return new WalletCredentialView(wc, credentialIssuanceService.get(tenantId, wc.getCredentialId()));
    }

    @Transactional
    public void deleteCredential(UUID tenantId, UUID walletId, UUID walletCredentialId) {
        get(tenantId, walletId);
        WalletCredential wc = walletCredentialRepository.findByTenantIdAndWalletIdAndId(tenantId, walletId, walletCredentialId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found in wallet: " + walletCredentialId));
        walletCredentialRepository.delete(wc);
        audit(tenantId, "WALLET_CREDENTIAL_DELETED", walletId);
    }

    @Transactional
    public WalletCredentialView recategorize(UUID tenantId, UUID walletId, UUID walletCredentialId, String category) {
        get(tenantId, walletId);
        WalletCredential wc = walletCredentialRepository.findByTenantIdAndWalletIdAndId(tenantId, walletId, walletCredentialId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found in wallet: " + walletCredentialId));
        wc.recategorize(category);
        return new WalletCredentialView(wc, credentialIssuanceService.get(tenantId, wc.getCredentialId()));
    }

    @Transactional
    public WalletCredentialView claim(UUID tenantId, UUID walletId, String preAuthorizedCode) {
        Wallet wallet = get(tenantId, walletId);

        Oid4vciSessionService.TokenResult tokenResult = oid4vciSessionService.issueToken(tenantId, preAuthorizedCode);
        Oid4vciSessionService.CredentialResult credentialResult =
                oid4vciSessionService.redeemCredential(tenantId, tokenResult.accessToken());
        Credential credential = credentialResult.credential();

        walletCredentialRepository.findByTenantIdAndWalletIdAndCredentialId(tenantId, wallet.getId(), credential.getId())
                .ifPresent(existing -> {
                    throw new ApiException(ApiErrorCode.RESOURCE_CONFLICT,
                            "This wallet already holds this credential: " + credential.getId());
                });

        WalletCredential walletCredential = new WalletCredential(tenantId, wallet.getId(), credential.getId(), null);
        walletCredentialRepository.save(walletCredential);
        audit(tenantId, "WALLET_CREDENTIAL_RECEIVED", wallet.getId());
        return new WalletCredentialView(walletCredential, credential);
    }

    @Transactional
    public WalletPresentation presentCredential(UUID tenantId, UUID walletId, UUID walletCredentialId,
                                                 String audience, String nonce) {
        Wallet wallet = get(tenantId, walletId);
        WalletCredential wc = walletCredentialRepository.findByTenantIdAndWalletIdAndId(tenantId, walletId, walletCredentialId)
                .orElseThrow(() -> new ApiException(ApiErrorCode.RESOURCE_NOT_FOUND,
                        "Credential not found in wallet: " + walletCredentialId));
        Credential credential = credentialIssuanceService.get(tenantId, wc.getCredentialId());
        Did holderDid = didService.get(tenantId, wallet.getDidId());
        ManagedKey signingKey = keyService.getActiveKeyForDid(holderDid.getId());

        WalletPresentationSigner.SignedPresentation signed = WalletPresentationSigner.sign(
                tenantId, holderDid, audience, nonce, List.of(credential.getCredentialJwt()),
                Instant.now().plus(PRESENTATION_TTL), signingKey, keyService);

        WalletPresentation presentation = new WalletPresentation(tenantId, wallet.getId(), wc.getId(), audience, nonce,
                signed.jwt());
        walletPresentationRepository.save(presentation);
        audit(tenantId, "WALLET_PRESENTATION_GENERATED", wallet.getId());
        return presentation;
    }

    public List<WalletPresentation> listPresentations(UUID tenantId, UUID walletId) {
        get(tenantId, walletId);
        return walletPresentationRepository.findByTenantIdAndWalletId(tenantId, walletId);
    }

    private void audit(UUID tenantId, String action, UUID walletId) {
        auditEventPublisher.publish(AuditEvent.of(
                tenantId,
                CurrentUserContext.getUserId(),
                action,
                "WALLET",
                walletId.toString(),
                "SUCCESS",
                Map.of()
        ));
    }

    /** Pairs a {@link WalletCredential} reference row with the underlying, authoritative {@link Credential}. */
    public record WalletCredentialView(WalletCredential walletCredential, Credential credential) {
    }
}
