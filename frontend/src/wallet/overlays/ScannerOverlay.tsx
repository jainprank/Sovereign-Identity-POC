import { useEffect, useRef, useState } from "react";
import jsQR from "jsqr";
import { Icon } from "../components/icons";
import type { ScanPurpose } from "../uiTypes";

interface Props {
  purpose: ScanPurpose;
  onClose: () => void;
  onDetect: (text: string) => void;
}

/**
 * Real camera-based QR scanning (getUserMedia + jsQR sampling video frames onto a hidden
 * canvas) — replaces the design mock's 2200ms `setTimeout` fake detection. Falls back to a
 * manual paste field when the camera is unavailable/denied, since that's also how an issuer's
 * offer link or a verifier's request URI would arrive outside a live camera (e.g. copied from
 * another device).
 */
export function ScannerOverlay({ purpose, onClose, onDetect }: Props) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const streamRef = useRef<MediaStream | null>(null);
  const rafRef = useRef<number | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manual, setManual] = useState("");
  const [showManual, setShowManual] = useState(false);

  useEffect(() => {
    let stopped = false;

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        setCameraError("Camera not available on this device.");
        setShowManual(true);
        return;
      }
      try {
        const stream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: "environment" } });
        if (stopped) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
        tick();
      } catch {
        setCameraError("Camera access was denied.");
        setShowManual(true);
      }
    }

    function tick() {
      const video = videoRef.current;
      const canvas = canvasRef.current;
      if (video && canvas && video.readyState === video.HAVE_ENOUGH_DATA) {
        const ctx = canvas.getContext("2d");
        if (ctx) {
          canvas.width = video.videoWidth;
          canvas.height = video.videoHeight;
          ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
          const image = ctx.getImageData(0, 0, canvas.width, canvas.height);
          const code = jsQR(image.data, image.width, image.height);
          if (code && code.data) {
            onDetect(code.data);
            return;
          }
        }
      }
      rafRef.current = requestAnimationFrame(tick);
    }

    start();
    return () => {
      stopped = true;
      if (rafRef.current) cancelAnimationFrame(rafRef.current);
      streamRef.current?.getTracks().forEach((t) => t.stop());
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const submitManual = () => {
    if (manual.trim()) onDetect(manual.trim());
  };

  return (
    <div className="idt-overlay">
      <div className="idt-ovheader">
        <button className="idt-iconbtn idt-iconbtn--ghost" aria-label="Close" onClick={onClose}><Icon name="close" size={18} /></button>
        <div className="idt-ovtitle" style={{ color: "#fff" }}>{purpose === "claim" ? "Scan offer" : "Scan request"}</div>
        <div style={{ width: 40 }} />
      </div>
      <div className="idt-scanner">
        <div className="idt-scanner__cam">
          <video ref={videoRef} muted playsInline style={{ width: "100%", height: "100%", objectFit: "cover" }} />
        </div>
        <canvas ref={canvasRef} style={{ display: "none" }} />
        {!showManual && (
          <div className="idt-scan-frame">
            <i className="tl" /><i className="tr" /><i className="bl" /><i className="br" />
            <div className="idt-scan-line" />
          </div>
        )}
        <div className="idt-scanner__hint">
          {cameraError ?? (purpose === "claim"
            ? "Point your camera at the issuer's QR code"
            : "Point your camera at the verifier's QR code")}
        </div>
        <div className="idt-btnbar">
          {!showManual && (
            <button className="idt-btn idt-btn--ghost" onClick={() => setShowManual(true)}>Enter code manually</button>
          )}
          {showManual && (
            <>
              <input
                value={manual}
                onChange={(e) => setManual(e.target.value)}
                placeholder="Paste code or link here"
                style={{ width: "100%", height: 48, padding: "0 14px", border: "1px solid var(--line)", borderRadius: 8, fontSize: 14, background: "#fff" }}
              />
              <button className="idt-btn idt-btn--primary" onClick={submitManual} disabled={!manual.trim()}>Continue</button>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
