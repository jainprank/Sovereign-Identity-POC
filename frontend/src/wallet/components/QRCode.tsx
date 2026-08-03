import { useEffect, useState } from "react";
import QRCodeLib from "qrcode";

/** Renders a real QR code (replacing the design bundle's faux pseudo-random grid) encoding `value`. */
export function QRCode({ value }: { value: string }) {
  const [dataUrl, setDataUrl] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    QRCodeLib.toDataURL(value, { margin: 0, color: { dark: "#000000", light: "#00000000" } })
      .then((url) => { if (!cancelled) setDataUrl(url); })
      .catch(() => { if (!cancelled) setDataUrl(null); });
    return () => { cancelled = true; };
  }, [value]);

  if (!dataUrl) return <div style={{ width: "100%", height: "100%", background: "var(--surface-2)" }} />;
  return <img src={dataUrl} alt="Presentation QR code" style={{ width: "100%", height: "100%", imageRendering: "pixelated" }} />;
}
