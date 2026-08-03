import { Icon } from "./icons";
import type { Tab } from "../uiTypes";

export function TabBar({ tab, onTab, onScan }: { tab: Tab; onTab: (t: Tab) => void; onScan: () => void }) {
  return (
    <div className="idt-nav">
      <button className={`idt-tab ${tab === "wallet" ? "is-active" : ""}`} onClick={() => onTab("wallet")}>
        <Icon name="wallet" size={22} />Wallet
      </button>
      <button className="idt-tab idt-tab--scan" onClick={onScan}>
        <span className="idt-scanfab"><Icon name="scan" size={24} color="#fff" /></span>
        <span>Scan</span>
      </button>
      <button className={`idt-tab ${tab === "activity" ? "is-active" : ""}`} onClick={() => onTab("activity")}>
        <Icon name="activity" size={22} />Activity
      </button>
      <button className={`idt-tab ${tab === "identity" ? "is-active" : ""}`} onClick={() => onTab("identity")}>
        <Icon name="identity" size={22} />Identity
      </button>
    </div>
  );
}
