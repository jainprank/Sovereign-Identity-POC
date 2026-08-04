import { useState } from "react";
import { Icon } from "../components/icons";
import type { ScanPurpose } from "../uiTypes";

interface Props {
  purpose: ScanPurpose;
  onClose: () => void;
  onDetect: (text: string) => void;
}

/**
 * Manual code/link entry only — no live camera scanning. getUserMedia's permission prompt
 * round-trips through a native Android dialog (Capacitor's BridgeWebChromeClient) and the
 * subsequent per-frame capture+decode loop proved unreliable on constrained devices/emulators
 * (ANRs, stuck permission prompts). Pasting is also how an issuer's offer link or a verifier's
 * request URI would arrive outside a live camera anyway (e.g. copied from another device), so
 * this is the one reliable path rather than a fallback.
 */
export function ScannerOverlay({ purpose, onClose, onDetect }: Props) {
  const [manual, setManual] = useState("");

  const submitManual = () => {
    if (manual.trim()) onDetect(manual.trim());
  };

  return (
    <div className="idt-overlay">
      <div className="idt-ovheader">
        <button className="idt-iconbtn idt-iconbtn--ghost" aria-label="Close" onClick={onClose}><Icon name="close" size={18} /></button>
        <div className="idt-ovtitle" style={{ color: "#fff" }}>{purpose === "claim" ? "Add offer" : "Add request"}</div>
        <div style={{ width: 40 }} />
      </div>
      <div className="idt-scanner">
        <div className="idt-scanner__hint">
          {purpose === "claim"
            ? "Paste the issuer's offer link or code"
            : "Paste the verifier's request link or code"}
        </div>
        <div className="idt-btnbar">
          <input
            value={manual}
            onChange={(e) => setManual(e.target.value)}
            placeholder="Paste code or link here"
            style={{ width: "100%", height: 48, padding: "0 14px", border: "1px solid var(--line)", borderRadius: 8, fontSize: 14, background: "#fff" }}
          />
          <button className="idt-btn idt-btn--primary" onClick={submitManual} disabled={!manual.trim()}>Continue</button>
        </div>
      </div>
    </div>
  );
}
