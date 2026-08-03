import { useState } from "react";
import type { WalletResponse } from "../api/types";
import { BarMark, Icon } from "../components/icons";
import { formatDateTime } from "../lib/credentialDisplay";

interface Props {
  holderInitials: string;
  wallet: WalletResponse | null;
  credentialCount: number;
  shareCount: number;
  issueCount: number;
  onSignOut: () => void;
}

export function IdentityScreen({ holderInitials, wallet, credentialCount, shareCount, issueCount, onSignOut }: Props) {
  const [copied, setCopied] = useState(false);

  const copyDid = () => {
    if (!wallet) return;
    navigator.clipboard?.writeText(wallet.did).then(() => {
      setCopied(true);
      setTimeout(() => setCopied(false), 1600);
    });
  };

  return (
    <div className="idt-view">
      <div className="idt-header">
        <h1 className="idt-title">Identity</h1>
      </div>

      <div className="idt-profile-hero">
        <div className="idt-avatar">
          {holderInitials}
          <span className="vbadge"><Icon name="check" size={13} color="#fff" /></span>
        </div>
        <div className="idt-profile-name">Your wallet</div>
        {wallet && (
          <button className="idt-profile-did" onClick={copyDid} title={wallet.did}>
            <Icon name="key" size={13} />
            {copied ? "Copied!" : wallet.did}
          </button>
        )}
      </div>

      <div className="idt-stats">
        <div className="idt-stat"><div className="n">{credentialCount}</div><div className="l">Credentials</div></div>
        <div className="idt-stat"><div className="n">{shareCount}</div><div className="l">Shared</div></div>
        <div className="idt-stat"><div className="n">{issueCount}</div><div className="l">Issued</div></div>
      </div>

      <div className="idt-settings">
        <div className="idt-setrow">
          <span className="idt-setrow__ico"><Icon name="shieldCheck" size={17} /></span>
          <span className="idt-setrow__txt"><span className="k">Wallet status</span><span className="v">{wallet?.status ?? "—"}</span></span>
        </div>
        <div className="idt-setrow">
          <span className="idt-setrow__ico"><Icon name="cal" size={17} /></span>
          <span className="idt-setrow__txt"><span className="k">Created</span><span className="v">{wallet ? formatDateTime(wallet.createdAt) : "—"}</span></span>
        </div>
        <button className="idt-setrow" onClick={copyDid}>
          <span className="idt-setrow__ico"><Icon name="key" size={17} /></span>
          <span className="idt-setrow__txt"><span className="k">Wallet DID</span><span className="v">{wallet?.did ?? "—"}</span></span>
          <Icon name="copy" size={16} />
        </button>
        <button className="idt-setrow" onClick={onSignOut}>
          <span className="idt-setrow__ico"><Icon name="lock" size={17} /></span>
          <span className="idt-setrow__txt"><span className="k">Sign out</span></span>
          <Icon name="chevR" size={16} />
        </button>
      </div>

      <div className="idt-secured">
        <div className="bars"><BarMark size={14} /></div>
        <div>Secured by The Hashgraph Group AG</div>
      </div>

      <div className="idt-scroll-pad" />
    </div>
  );
}
