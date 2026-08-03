import type { AxiosInstance } from "axios";
import type { PresentationResponse, WalletCredentialResponse, WalletResponse } from "./types";

// Thin wrapper around the real EPIC 8/10/11 backend endpoints in
// com.idtrust.wallet.WalletController — no mocked data. `api` is expected to be an
// AxiosInstance already carrying the caller's bearer token + X-Tenant-ID header
// (see src/lib/api.ts#createApiClient).

export async function listWallets(api: AxiosInstance): Promise<WalletResponse[]> {
  const { data } = await api.get<WalletResponse[]>("/api/v1/wallets");
  return data;
}

export async function createWallet(api: AxiosInstance, label: string): Promise<WalletResponse> {
  const { data } = await api.post<WalletResponse>("/api/v1/wallets", { label });
  return data;
}

/**
 * WalletController#list returns every wallet in the tenant, not just the caller's own (a
 * documented backend limitation — see WalletService's Javadoc). Until a per-holder "my wallet"
 * endpoint exists, a holder's wallet is simply the first one found, or a freshly created one.
 */
export async function ensureMyWallet(api: AxiosInstance): Promise<WalletResponse> {
  const wallets = await listWallets(api);
  if (wallets.length > 0) return wallets[0];
  return createWallet(api, "My Wallet");
}

export async function listCredentials(api: AxiosInstance, walletId: string): Promise<WalletCredentialResponse[]> {
  const { data } = await api.get<WalletCredentialResponse[]>(`/api/v1/wallets/${walletId}/credentials`);
  return data;
}

export async function deleteCredential(api: AxiosInstance, walletId: string, credentialId: string): Promise<void> {
  await api.delete(`/api/v1/wallets/${walletId}/credentials/${credentialId}`);
}

/** Re-fetches one credential fresh from the issuer's tenant — the honest equivalent of the design
 * mock's simulated "Check status" revocation check: `status` can have changed since it was last synced. */
export async function getCredential(api: AxiosInstance, walletId: string, credentialId: string): Promise<WalletCredentialResponse> {
  const { data } = await api.get<WalletCredentialResponse>(`/api/v1/wallets/${walletId}/credentials/${credentialId}`);
  return data;
}

/** Lets a holder organize their own wallet (FR-WAL-008) — feeds directly into credentialDisplay.ts#titleFor's category-first display. */
export async function recategorizeCredential(api: AxiosInstance, walletId: string, credentialId: string, category: string | null): Promise<WalletCredentialResponse> {
  const { data } = await api.put<WalletCredentialResponse>(`/api/v1/wallets/${walletId}/credentials/${credentialId}`, { category });
  return data;
}

/** `preAuthorizedCode` is redeemed via the in-process OID4VCI token+credential exchange (FR-WAL-004/005). */
export async function claimCredential(api: AxiosInstance, walletId: string, preAuthorizedCode: string): Promise<WalletCredentialResponse> {
  const { data } = await api.post<WalletCredentialResponse>(`/api/v1/wallets/${walletId}/claim`, { preAuthorizedCode });
  return data;
}

/**
 * Signs and records a VP JWT (FR-WAL-011/012/017). `disclosedClaims` only matters for an
 * SD-JWT VC credential (FR-WAL-014, Selective Disclosure) — omit/empty to disclose everything.
 */
export async function presentCredential(
  api: AxiosInstance,
  walletId: string,
  walletCredentialId: string,
  audience: string,
  nonce: string,
  disclosedClaims?: string[],
): Promise<PresentationResponse> {
  const { data } = await api.post<PresentationResponse>(
    `/api/v1/wallets/${walletId}/credentials/${walletCredentialId}/present`,
    { audience, nonce, disclosedClaims: disclosedClaims && disclosedClaims.length > 0 ? disclosedClaims : null },
  );
  return data;
}

export async function listPresentations(api: AxiosInstance, walletId: string): Promise<PresentationResponse[]> {
  const { data } = await api.get<PresentationResponse[]>(`/api/v1/wallets/${walletId}/presentations`);
  return data;
}

/**
 * Parses a pasted or scanned OID4VCI credential offer into a bare pre-authorized code. Accepts
 * either the raw code, an `openid-credential-offer://` URI, or any URL carrying a
 * `pre-authorized_code` (nested inside a `credential_offer` query param, either as JSON or
 * itself a URL-encoded JSON blob) — matching what Oid4vciOfferGenerator produces server-side.
 */
export function extractPreAuthorizedCode(input: string): string | null {
  const trimmed = input.trim();
  if (!trimmed) return null;
  try {
    const url = new URL(trimmed);
    const offerParam = url.searchParams.get("credential_offer");
    if (offerParam) {
      const offer = JSON.parse(offerParam);
      const code = offer?.grants?.["urn:ietf:params:oauth:grant-type:pre-authorized_code"]?.["pre-authorized_code"];
      if (typeof code === "string" && code) return code;
    }
  } catch {
    // Not a URL / not JSON — fall through and treat the input as a bare code.
  }
  return trimmed;
}
