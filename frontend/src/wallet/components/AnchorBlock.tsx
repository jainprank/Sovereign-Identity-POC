import type { AnchorInfo } from "../lib/credentialDisplay";
import { BarMark, Icon } from "./icons";

export function AnchorRows({ info }: { info: AnchorInfo }) {
  return (
    <div className="idt-anchor__rows">
      {info.rows.map((r) => (
        <div className="idt-anchor__row" key={r.k}>
          <span className="k">{r.k}</span>
          <span className="v">{r.v}</span>
        </div>
      ))}
      <div className="idt-anchor__row">
        <span className="k">Status</span>
        <span className="v"><span className="idt-chip-ok"><Icon name="check" size={13} />{info.okLabel}</span></span>
      </div>
    </div>
  );
}

export function AnchorBlock({ info }: { info: AnchorInfo }) {
  return (
    <div className="idt-anchor">
      <div className="idt-anchor__head">
        <BarMark size={16} color="var(--indigo)" />
        <div>
          <div className="t">{info.title}</div>
          <div className="s">{info.subtitle}</div>
        </div>
      </div>
      <AnchorRows info={info} />
    </div>
  );
}
