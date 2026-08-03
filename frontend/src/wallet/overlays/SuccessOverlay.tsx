import { AnchorRows } from "../components/AnchorBlock";
import { Icon } from "../components/icons";
import type { SuccessInfo } from "../uiTypes";

export function SuccessOverlay({ info, onDone }: { info: SuccessInfo; onDone: () => void }) {
  return (
    <div className="idt-success">
      <div className="idt-success__ring">
        <svg width="40" height="40" viewBox="0 0 24 24" fill="none" className="idt-check-draw">
          <path d="M5 12.5l4.5 4.5L19 7" stroke="#fff" strokeWidth={2.5} strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
      <h2>{info.heading}</h2>
      <p>{info.detail}</p>
      <div className="idt-success__receipt">
        <AnchorRows info={info.anchor} />
      </div>
      <div className="idt-btnbar" style={{ position: "relative", background: "none", padding: "26px 0 0" }}>
        <button className="idt-btn idt-btn--primary" onClick={onDone}>
          <Icon name="check" size={17} color="#fff" />Done
        </button>
      </div>
    </div>
  );
}
