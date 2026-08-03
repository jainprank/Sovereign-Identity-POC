import { useMemo, useState } from "react";
import type { AxiosInstance } from "axios";
import { presentCredential } from "../api/walletApi";
import type { PresentationResponse, WalletCredentialResponse } from "../api/types";
import { Icon } from "../components/icons";
import { Toggle } from "../components/Toggle";
import { formatClaimKey, issuerShort, titleFor } from "../lib/credentialDisplay";
import type { ParsedVpRequest } from "../lib/oid4vp";

interface Props {
  api: AxiosInstance;
  walletId: string;
  request: ParsedVpRequest;
  credentials: WalletCredentialResponse[];
  onClose: () => void;
  onComplete: (presentation: PresentationResponse, wc: WalletCredentialResponse) => void;
}

/**
 * Real OID4VP consent: `request.schemaId`/`requiredClaims` come straight off the verifier's
 * policy (VerificationService#buildPresentationDefinition — see PolicyDefinition, which has no
 * "optional claim" concept), so every listed field is one the verifier requires — there's no
 * honest equivalent of the design mock's per-field opt-out toggle. Toggles are shown locked-on
 * to communicate "this will be shared" without fabricating an ability to withhold it.
 */
export function ConsentOverlay({ api, walletId, request, credentials, onClose, onComplete }: Props) {
  const matches = useMemo(
    () => credentials.filter((wc) => !request.schemaId || wc.credential.schemaId === request.schemaId),
    [credentials, request.schemaId],
  );
  const [selectedId, setSelectedId] = useState<string | null>(matches.length > 0 ? matches[0].id : null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selected = matches.find((wc) => wc.id === selectedId) ?? null;
  const verifierTag = issuerShort(request.clientId);

  const submit = async () => {
    if (!selected) return;
    setSubmitting(true);
    setError(null);
    try {
      const presentation = await presentCredential(
        api, walletId, selected.id, request.clientId, request.nonce,
        request.requiredClaims.length > 0 ? request.requiredClaims : undefined,
      );
      await api.post(request.responseUri, { vpJwt: presentation.vpJwt });
      onComplete(presentation, selected);
    } catch {
      setError("Couldn't complete this presentation. Please try again.");
      setSubmitting(false);
    }
  };

  return (
    <div className="idt-overlay">
      <div className="idt-ovheader">
        <button className="idt-iconbtn" aria-label="Close" onClick={onClose}><Icon name="close" size={18} /></button>
        <div className="idt-ovtitle">Share credential</div>
        <div style={{ width: 40 }} />
      </div>
      <div className="idt-consent">
        <div className="idt-consent__scroll">
          <div className="idt-verifier">
            <div className="idt-verifier__logo">{verifierTag}</div>
            <div className="idt-verifier__name">Verifier request</div>
            <div className="idt-verifier__sub">{request.clientId}</div>
            <div className="idt-verifier__req">Requests {request.requiredClaims.length || "your"} attribute{request.requiredClaims.length === 1 ? "" : "s"}</div>
          </div>

          {matches.length === 0 && (
            <div className="idt-error">You don't have a credential matching this request.</div>
          )}

          {matches.length > 1 && (
            <div className="idt-attr-group">
              <div className="lbl">Choose a credential</div>
              {matches.map((wc) => (
                <button key={wc.id} className="idt-setrow" onClick={() => setSelectedId(wc.id)}>
                  <span className="idt-setrow__ico"><Icon name="cert" size={17} /></span>
                  <span className="idt-setrow__txt"><span className="k">{titleFor(wc)}</span></span>
                  {selectedId === wc.id && <Icon name="check" size={16} color="var(--indigo)" />}
                </button>
              ))}
            </div>
          )}

          {selected && (
            <div className="idt-disclose-card">
              <div className="idt-disclose-card__h">
                <span className="using">Using</span>
                <span className="cn">{titleFor(selected)}</span>
              </div>
              {(request.requiredClaims.length > 0 ? request.requiredClaims : ["All attributes"]).map((claim) => (
                <div className="idt-discl" key={claim}>
                  <div className="idt-discl__ico"><Icon name="shieldCheck" size={17} /></div>
                  <div className="idt-discl__txt">
                    <div className="k">{claim === "All attributes" ? claim : formatClaimKey(claim)}</div>
                    <div className="v">Required by verifier</div>
                  </div>
                  <Toggle on locked />
                </div>
              ))}
            </div>
          )}

          <div className="idt-privacy-note">
            <Icon name="shieldCheck" size={16} />
            <span>A signed presentation is created on this device and sent directly to the verifier. Your wallet keeps a record of every share in Activity.</span>
          </div>

          {error && <div className="idt-error">{error}</div>}
        </div>
        <div className="idt-btnbar">
          <button className="idt-btn idt-btn--primary" onClick={submit} disabled={!selected || submitting}>
            {submitting ? "Sharing…" : "Share securely"}
          </button>
          <button className="idt-btn idt-btn--ghost" onClick={onClose} disabled={submitting}>Decline</button>
        </div>
      </div>
    </div>
  );
}
