import { useEffect, useRef, useState } from "react";
import jsQR from "jsqr";
import { Icon } from "../components/icons";
import type { ScanPurpose } from "../uiTypes";

interface Props {
  purpose: ScanPurpose;
  onClose: () => void;
  onDetect: (text: string) => void;
}

/** getUserMedia's permission prompt round-trips through a native Android dialog (see
 * MainActivity/Capacitor's BridgeWebChromeClient); on a slow device that can take a while, or in
 * rare cases never resolve. Racing it against a timeout means a stuck permission flow degrades to
 * the manual-entry fallback instead of leaving the whole overlay unresponsive. */
const CAMERA_START_TIMEOUT_MS = 8000;

/** Full-resolution capture+decode on every animation frame (~60/sec) is what actually pegged the
 * WebView's JS thread hard enough to trigger an ANR on a constrained device/emulator during
 * testing - QR content doesn't change fast enough to need that; sampling a few times a second
 * still detects a held-up code well within human reaction time. */
const SCAN_INTERVAL_MS = 200;

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
  const intervalRef = useRef<number | null>(null);
  const [cameraError, setCameraError] = useState<string | null>(null);
  const [manual, setManual] = useState("");
  const [showManual, setShowManual] = useState(false);

  useEffect(() => {
    let stopped = false;

    function fail(message: string) {
      if (stopped) return;
      setCameraError(message);
      setShowManual(true);
    }

    async function start() {
      if (!navigator.mediaDevices?.getUserMedia) {
        fail("Camera not available on this device.");
        return;
      }
      let timedOut = false;
      // Kept separate from the race below (rather than relying on Promise.race to discard it) so
      // a getUserMedia call that resolves *after* the timeout already fired still gets its stream
      // stopped immediately instead of silently holding the camera open with nothing watching it.
      const request = navigator.mediaDevices
        .getUserMedia({ video: { facingMode: "environment" } })
        .then((stream) => {
          if (timedOut) {
            stream.getTracks().forEach((t) => t.stop());
          }
          return stream;
        });
      try {
        const stream = await Promise.race([
          request,
          new Promise<never>((_, reject) =>
            setTimeout(() => {
              timedOut = true;
              reject(new Error("camera-start-timeout"));
            }, CAMERA_START_TIMEOUT_MS),
          ),
        ]);
        if (stopped) {
          stream.getTracks().forEach((t) => t.stop());
          return;
        }
        streamRef.current = stream;
        if (videoRef.current) {
          videoRef.current.srcObject = stream;
          await videoRef.current.play();
        }
        intervalRef.current = window.setInterval(tick, SCAN_INTERVAL_MS);
      } catch {
        fail("Camera access was denied.");
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
            if (intervalRef.current) window.clearInterval(intervalRef.current);
            onDetect(code.data);
          }
        }
      }
    }

    start();
    return () => {
      stopped = true;
      if (intervalRef.current) window.clearInterval(intervalRef.current);
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
