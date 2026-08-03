// Presentation-layer helpers that turn the real WalletCredentialResponse/CredentialResponse
// shape (schemaId + issuerDid + a flat claims map, format, status — nothing decorative) into
// the visual metadata the design calls for (card color, icon, masked/sensitive fields).
//
// A holder-scoped session has no access to /api/v1/credential-schemas/{id} or
// /api/v1/hedera/anchors (both PLATFORM_ADMIN/ORG_ADMIN/ISSUER/VERIFIER-only — see
// CredentialSchemaController / HederaAnchorController), so there is no schema display name and
// no real Hedera topic/tx id to show here. Everything below is derived only from fields the
// wallet API actually returns.

import type { CredentialResponse, PresentationResponse, WalletCredentialResponse } from "../api/types";

const VARIANTS = ["cred--navy", "cred--indigo", "cred--black", "cred--deep", "cred--coral"];

const SENSITIVE_KEY_PATTERN = /birth|ssn|social|passport|licen[cs]e|document|national|idnumber|id_number/i;

const ICON_BY_KEYWORD: Array<[RegExp, string]> = [
  [/passport|travel/i, "globe"],
  [/licen[cs]e|driv/i, "car"],
  [/degree|diploma|education|university|eth/i, "cert"],
  [/age|18|adult/i, "fingerprint"],
  [/employ|work/i, "building"],
  [/health|insurance/i, "shield"],
  [/resid|address/i, "doc"],
  [/id|identity|government/i, "shieldCheck"],
];

function hashString(s: string): number {
  let h = 0;
  for (let i = 0; i < s.length; i++) h = (h * 31 + s.charCodeAt(i)) | 0;
  return Math.abs(h);
}

export function variantFor(walletCredentialId: string): string {
  return VARIANTS[hashString(walletCredentialId) % VARIANTS.length];
}

export function titleFor(wc: WalletCredentialResponse): string {
  if (wc.category) return wc.category;
  const claims = wc.credential.claims || {};
  const nameLike = (claims["credentialName"] || claims["name"] || claims["title"]) as string | undefined;
  if (typeof nameLike === "string" && nameLike) return nameLike;
  return wc.credential.format === "SD_JWT_VC" ? "Selective Disclosure Credential" : "Verifiable Credential";
}

export function iconFor(wc: WalletCredentialResponse): string {
  const haystack = `${wc.category ?? ""} ${titleFor(wc)}`;
  for (const [pattern, icon] of ICON_BY_KEYWORD) {
    if (pattern.test(haystack)) return icon;
  }
  return "cert";
}

/** DIDs don't carry an org name; a short 2-char tag is derived from the DID's own identifier, same idea as the design's issuer initials. */
export function issuerShort(issuerDid: string): string {
  const parts = issuerDid.split(":");
  const tail = parts[parts.length - 1] || issuerDid;
  const alnum = tail.replace(/[^a-zA-Z0-9]/g, "");
  return (alnum.slice(0, 2) || "ID").toUpperCase();
}

export function protocolLabel(format: CredentialResponse["format"]): string {
  return format === "SD_JWT_VC" ? "OID4VCI · SD-JWT" : "OID4VCI · JWT-VC";
}

export function statusLabel(status: CredentialResponse["status"]): { text: string; dotColor: string } {
  switch (status) {
    case "ISSUED":
      return { text: "Verified", dotColor: "#6BE3A8" };
    case "SUSPENDED":
      return { text: "Suspended", dotColor: "#FC8A65" };
    case "EXPIRED":
      return { text: "Expired", dotColor: "#D8D8DE" };
    case "REVOKED":
    default:
      return { text: "Revoked", dotColor: "#D8D8DE" };
  }
}

export function formatClaimKey(key: string): string {
  const spaced = key.replace(/([a-z0-9])([A-Z])/g, "$1 $2").replace(/_/g, " ");
  return spaced.charAt(0).toUpperCase() + spaced.slice(1);
}

export interface DisplayClaim {
  key: string;
  label: string;
  value: string;
  sensitive: boolean;
}

export function claimsFor(credential: CredentialResponse): DisplayClaim[] {
  return Object.entries(credential.claims || {})
    .filter(([key]) => key !== "vc" && key !== "sub" && key !== "iss")
    .map(([key, value]) => ({
      key,
      label: formatClaimKey(key),
      value: typeof value === "string" ? value : JSON.stringify(value),
      sensitive: SENSITIVE_KEY_PATTERN.test(key),
    }));
}

export function formatDate(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleDateString(undefined, { day: "2-digit", month: "2-digit", year: "2-digit" });
}

export function formatDateTime(iso: string | null | undefined): string {
  if (!iso) return "—";
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return iso;
  return d.toLocaleString(undefined, {
    day: "2-digit", month: "2-digit", year: "2-digit",
    hour: "2-digit", minute: "2-digit", second: "2-digit",
  }) + " " + Intl.DateTimeFormat().resolvedOptions().timeZone;
}

export function truncateMiddle(value: string, head = 10, tail = 6): string {
  if (value.length <= head + tail + 1) return value;
  return `${value.slice(0, head)}…${value.slice(-tail)}`;
}

export interface AnchorInfo {
  title: string;
  subtitle: string;
  rows: Array<{ k: string; v: string; mono?: boolean }>;
  okLabel: string;
}

/**
 * A holder session can't reach /api/v1/hedera/anchors (PLATFORM_ADMIN/ORG_ADMIN/VERIFIER-only),
 * so there's no real Hedera topic/tx id to show per credential. This surfaces what the wallet
 * API *does* return — issuer signature format, issuance time, and the credential's own content
 * hash — as the tamper-evidence receipt instead of fabricating ledger coordinates.
 */
export function anchorForCredential(c: CredentialResponse): AnchorInfo {
  return {
    title: "Credential integrity",
    subtitle: "Signed by the issuer · content-addressed",
    rows: [
      { k: "Protocol", v: protocolLabel(c.format) },
      { k: "Issued", v: formatDateTime(c.issuedAt) },
      { k: "Credential hash", v: truncateMiddle(c.credentialHash, 10, 6), mono: true },
    ],
    okLabel: statusLabel(c.status).text,
  };
}

export function anchorForPresentation(p: PresentationResponse): AnchorInfo {
  return {
    title: "Presentation receipt",
    subtitle: "Signed VP JWT recorded in your trust log",
    rows: [
      { k: "Protocol", v: "OID4VP" },
      { k: "Presented", v: formatDateTime(p.presentedAt) },
      { k: "Audience", v: p.audience },
      { k: "Reference", v: truncateMiddle(p.id, 8, 6), mono: true },
    ],
    okLabel: "Recorded",
  };
}
