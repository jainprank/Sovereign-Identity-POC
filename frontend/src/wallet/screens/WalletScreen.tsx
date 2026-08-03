import type { WalletCredentialResponse } from "../api/types";
import { CredentialCard } from "../components/CredentialCard";
import { BarMark, Icon } from "../components/icons";

interface Props {
  holderInitials: string;
  did: string;
  credentials: WalletCredentialResponse[];
  loading: boolean;
  error: string | null;
  unreadCount: number;
  onOpenCred: (id: string) => void;
  onAdd: () => void;
  onOpenNotifications: () => void;
}

export function WalletScreen({ holderInitials, did, credentials, loading, error, unreadCount, onOpenCred, onAdd, onOpenNotifications }: Props) {
  return (
    <div className="idt-view">
      <div className="idt-header">
        <div className="idt-brand">
          <BarMark size={20} />
          <span className="idt-brand-name">ID Trust</span>
        </div>
        <button className="idt-iconbtn" aria-label="Notifications" onClick={onOpenNotifications} style={{ position: "relative" }}>
          <Icon name="bell" size={19} />
          {unreadCount > 0 && (
            <span style={{ position: "absolute", top: 6, right: 7, width: 8, height: 8, borderRadius: 999, background: "var(--coral)" }} />
          )}
        </button>
      </div>

      <div className="idt-idcard">
        <div className="idt-avatar">
          {holderInitials}
          <span className="vbadge"><Icon name="check" size={11} color="#fff" /></span>
        </div>
        <div>
          <div className="idt-id-name">Your wallet</div>
          <div className="idt-id-sub"><span className="idt-did">{did}</span></div>
        </div>
      </div>

      <div className="idt-section">
        <h3>Credentials</h3>
        <span className="count">{credentials.length}</span>
      </div>

      {loading && <div className="idt-loading">Loading your credentials…</div>}
      {error && <div className="idt-error">{error}</div>}

      {!loading && !error && (
        <div className="idt-cards">
          {credentials.length === 0 && <div className="idt-empty">No credentials yet — add one below.</div>}
          {credentials.map((wc) => (
            <CredentialCard key={wc.id} wc={wc} onClick={() => onOpenCred(wc.id)} />
          ))}
        </div>
      )}

      <button className="idt-add" onClick={onAdd}>
        <span className="plus"><Icon name="plus" size={18} color="var(--indigo)" /></span>
        <span>
          <b>Add credential</b>
          <small>Redeem an issuer's offer</small>
        </span>
      </button>

      <div className="idt-scroll-pad" />
    </div>
  );
}
