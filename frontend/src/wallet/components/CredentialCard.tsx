import type { WalletCredentialResponse } from "../api/types";
import { formatDate, iconFor, issuerShort, statusLabel, titleFor, variantFor } from "../lib/credentialDisplay";
import { BarcodeStrip, Icon } from "./icons";

export function CredentialCard({ wc, onClick }: { wc: WalletCredentialResponse; onClick?: () => void }) {
  const variant = variantFor(wc.id);
  const status = statusLabel(wc.credential.status);
  const Tag = onClick ? "button" : "div";
  return (
    <Tag className={`idt-cred ${variant}`} onClick={onClick}>
      <div className="idt-cred__bars"><BarcodeStrip color="#fff" /></div>
      <div className="idt-cred__top">
        <div className="idt-cred__issuer">
          <div className="idt-cred__issuer-logo">{issuerShort(wc.credential.issuerDid)}</div>
          <div className="idt-cred__issuer-name">Issued via<br />verified DID</div>
        </div>
        <Icon name={iconFor(wc)} size={20} color="#fff" />
      </div>
      <div className="idt-cred__body">
        <div className="idt-cred__name">{titleFor(wc)}</div>
        <div className="idt-cred__meta">
          <span>Received <b>{formatDate(wc.receivedAt)}</b></span>
          {wc.credential.expiresAt && <span>Expires <b>{formatDate(wc.credential.expiresAt)}</b></span>}
        </div>
        <div style={{ marginTop: 10 }}>
          <span className="idt-cred__status"><span className="dot" style={{ background: status.dotColor }} />{status.text}</span>
        </div>
      </div>
    </Tag>
  );
}
