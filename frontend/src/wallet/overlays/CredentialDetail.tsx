import { useState } from "react";
import type { AxiosInstance } from "axios";
import { deleteCredential, getCredential, recategorizeCredential } from "../api/walletApi";
import type { WalletCredentialResponse } from "../api/types";
import { AnchorBlock } from "../components/AnchorBlock";
import { CredentialCard } from "../components/CredentialCard";
import { Icon } from "../components/icons";
import { anchorForCredential, claimsFor, formatDate, statusLabel } from "../lib/credentialDisplay";

interface Props {
  api: AxiosInstance;
  walletId: string;
  wc: WalletCredentialResponse;
  onClose: () => void;
  onPresent: (wc: WalletCredentialResponse) => void;
  onUpdated: (wc: WalletCredentialResponse) => void;
  onDeleted: (walletCredentialId: string) => void;
}

export function CredentialDetail({ api, walletId, wc, onClose, onPresent, onUpdated, onDeleted }: Props) {
  const [revealed, setRevealed] = useState<Set<string>>(new Set());
  const [checking, setChecking] = useState(false);
  const [checkNote, setCheckNote] = useState<string | null>(null);
  const [category, setCategory] = useState(wc.category ?? "");
  const [savingCategory, setSavingCategory] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const claims = claimsFor(wc.credential);

  const toggle = (key: string) => {
    setRevealed((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key); else next.add(key);
      return next;
    });
  };

  const checkStatus = async () => {
    setChecking(true);
    setCheckNote(null);
    try {
      const fresh = await getCredential(api, walletId, wc.id);
      onUpdated(fresh);
      setCheckNote(`Status: ${statusLabel(fresh.credential.status).text} · checked just now`);
    } catch {
      setCheckNote("Couldn't reach the issuer to check status.");
    } finally {
      setChecking(false);
    }
  };

  const saveCategory = async () => {
    setSavingCategory(true);
    try {
      const fresh = await recategorizeCredential(api, walletId, wc.id, category.trim() || null);
      onUpdated(fresh);
    } finally {
      setSavingCategory(false);
    }
  };

  const handleDelete = async () => {
    if (!confirmDelete) {
      setConfirmDelete(true);
      return;
    }
    setDeleting(true);
    try {
      await deleteCredential(api, walletId, wc.id);
      onDeleted(wc.id);
      onClose();
    } catch {
      setDeleting(false);
      setConfirmDelete(false);
    }
  };

  return (
    <div className="idt-overlay">
      <div className="idt-ovheader">
        <button className="idt-iconbtn" aria-label="Close" onClick={onClose}><Icon name="close" size={18} /></button>
        <div className="idt-ovtitle">Credential</div>
        <div style={{ width: 40 }} />
      </div>
      <div className="idt-view">
        <div className="idt-detail-hero">
          <CredentialCard wc={wc} />
        </div>
        <div className="idt-detail-body">
          <div className="idt-attr-group">
            <div className="lbl">Details</div>
            <div className="idt-attr">
              <span className="k">Received</span>
              <span className="v">{formatDate(wc.receivedAt)}</span>
            </div>
            <div className="idt-attr">
              <span className="k">Expires</span>
              <span className="v">{formatDate(wc.credential.expiresAt)}</span>
            </div>
            <div className="idt-attr">
              <span className="k">Issuer DID</span>
              <span className="v" style={{ fontFamily: "var(--font-mono)", fontSize: 11.5 }}>{wc.credential.issuerDid}</span>
            </div>
            <div className="idt-attr">
              <span className="k">Status</span>
              <span className="v">
                {statusLabel(wc.credential.status).text}
                <button className="reveal" onClick={checkStatus} disabled={checking}>{checking ? "Checking…" : "Check now"}</button>
              </span>
            </div>
            {checkNote && <div style={{ fontSize: 11.5, color: "var(--muted)", marginTop: -4, paddingBottom: 8 }}>{checkNote}</div>}
          </div>

          <div className="idt-attr-group">
            <div className="lbl">Organize</div>
            <div style={{ display: "flex", gap: 8 }}>
              <input
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                placeholder="e.g. Travel, Work, Personal"
                style={{ flex: 1, height: 42, padding: "0 12px", border: "1px solid var(--line)", borderRadius: 8, fontSize: 13.5 }}
              />
              <button
                className="idt-btn idt-btn--ghost"
                style={{ width: "auto", height: 42, padding: "0 16px" }}
                onClick={saveCategory}
                disabled={savingCategory || category.trim() === (wc.category ?? "")}
              >
                {savingCategory ? "Saving…" : "Save"}
              </button>
            </div>
          </div>

          {claims.length > 0 && (
            <div className="idt-attr-group">
              <div className="lbl">Attributes</div>
              {claims.map((c) => (
                <div className="idt-attr" key={c.key}>
                  <span className="k">{c.label}</span>
                  <span className="v">
                    {c.sensitive && !revealed.has(c.key) ? (
                      <>
                        <span className="idt-mask">••••••••</span>
                        <button className="reveal" onClick={() => toggle(c.key)}>Show</button>
                      </>
                    ) : (
                      <>
                        {c.value}
                        {c.sensitive && <button className="reveal" onClick={() => toggle(c.key)}>Hide</button>}
                      </>
                    )}
                  </span>
                </div>
              ))}
            </div>
          )}

          <AnchorBlock info={anchorForCredential(wc.credential)} />

          <button className="idt-btn idt-btn--danger" style={{ marginTop: 22 }} onClick={handleDelete} disabled={deleting}>
            <Icon name="close" size={16} />
            {deleting ? "Removing…" : confirmDelete ? "Tap again to confirm removal" : "Remove from wallet"}
          </button>
        </div>
        <div className="idt-scroll-pad" />
      </div>
      <div className="idt-btnbar">
        <button className="idt-btn idt-btn--primary" onClick={() => onPresent(wc)}>
          <Icon name="scan" size={17} color="#fff" />Present credential
        </button>
      </div>
    </div>
  );
}
