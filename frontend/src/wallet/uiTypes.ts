import type { AnchorInfo } from "./lib/credentialDisplay";
import type { ParsedVpRequest } from "./lib/oid4vp";

export type Tab = "wallet" | "activity" | "identity";

/**
 * The real backend's OID4VP flow is verifier-initiated: a verifier generates the QR (see
 * Oid4vpRequestGenerator), and the holder scans it. There's no backend capability for the
 * reverse ("holder shows a QR, verifier scans it"), so unlike the design mock there's a single
 * scan flow rather than separate "Present" (show my QR) and "Scan" (read a QR) overlays —
 * `purpose` just tells the root what to do with whatever the scanner decodes.
 */
export type ScanPurpose = "claim" | "present";

export type OverlayState =
  | { t: "cred"; walletCredentialId: string }
  | { t: "scan"; purpose: ScanPurpose }
  | { t: "consent"; request: ParsedVpRequest }
  | { t: "add" }
  | { t: "notifications" }
  | null;

export interface ActivityEvent {
  id: string;
  kind: "issue" | "share";
  icon: string;
  cls: string;
  title: string;
  desc: string;
  time: string; // ISO timestamp
  anchor: AnchorInfo;
}

export interface SuccessInfo {
  heading: string;
  detail: string;
  anchor: AnchorInfo;
}
