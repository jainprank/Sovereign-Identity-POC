// Mirrors backend/wallet DTOs (com.idtrust.wallet.WalletDtos, com.idtrust.credential.issuance.CredentialDtos).
// Kept intentionally minimal — only the fields the wallet UI reads.

export type WalletStatus = "ACTIVE" | "SUSPENDED" | "CLOSED";
export type CredentialFormat = "JWT_VC" | "SD_JWT_VC";
// Mirrors backend.credential.issuance.CredentialStatus exactly - there is no "ACTIVE" value on
// the backend; a freshly-issued, unrevoked credential's status is "ISSUED".
export type CredentialStatus = "ISSUED" | "SUSPENDED" | "REVOKED" | "EXPIRED";

export interface WalletResponse {
  id: string;
  tenantId: string;
  ownerUserId: string | null;
  did: string;
  label: string;
  status: WalletStatus;
  createdAt: string;
  updatedAt: string;
}

export interface CredentialResponse {
  id: string;
  tenantId: string;
  schemaId: string;
  issuerDid: string;
  subjectDid: string | null;
  format: CredentialFormat;
  credentialJwt: string;
  credentialHash: string;
  claims: Record<string, unknown>;
  status: CredentialStatus;
  statusReason: string | null;
  statusListId: string | null;
  statusListIndex: number | null;
  issuedAt: string;
  expiresAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface WalletCredentialResponse {
  id: string;
  walletId: string;
  category: string | null;
  receivedAt: string;
  credential: CredentialResponse;
}

export interface PresentationResponse {
  id: string;
  walletId: string;
  walletCredentialId: string;
  audience: string;
  vpJwt: string;
  presentedAt: string;
}

// Mirrors com.idtrust.notification.NotificationDtos — self-service, no role restriction (see
// NotificationController's Javadoc), so a HOLDER session can read its own feed directly.
export interface NotificationResponse {
  id: string;
  eventType: string;
  resourceType: string;
  resourceId: string;
  message: string;
  read: boolean;
  createdAt: string;
}
