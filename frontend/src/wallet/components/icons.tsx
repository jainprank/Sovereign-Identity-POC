// icons.tsx — THG bar-mark + Carbon-style stroke icon set for the ID Trust wallet.
// Ported 1:1 from the design bundle's app/icons.jsx.

const S = { fill: "none", stroke: "currentColor", strokeWidth: 2, strokeLinecap: "butt" as const, strokeLinejoin: "miter" as const };

export function BarMark({ size = 22, color = "currentColor" }: { size?: number; color?: string }) {
  const h = size;
  const w = size * 1.25;
  const bars: Array<[number, number]> = [
    [1, 3], [5.5, 1.4], [8.2, 2.2],
    [28, 2.2], [31.3, 1.4], [34, 3],
  ];
  const bridge = [14, 17, 20, 23];
  return (
    <svg width={w} height={h} viewBox="0 0 40 32" fill={color} aria-hidden="true" style={{ display: "block" }}>
      {bars.map(([x, bw], i) => <rect key={i} x={x} y="2" width={bw} height="28" />)}
      {bridge.map((x, i) => <rect key={"b" + i} x={x} y="13" width="1.4" height="6" />)}
    </svg>
  );
}

export function BarcodeStrip({ w = 130, h = 120, color = "currentColor" }: { w?: number; h?: number; color?: string }) {
  const widths = [3, 1, 2, 1, 1, 4, 1, 2, 3, 1, 1, 2, 1, 3, 1, 2, 1, 1, 4, 1, 2, 1, 3, 1];
  let x = 0;
  const gap = 2.4;
  const rects = widths.map((bw, i) => {
    const r = <rect key={i} x={x} y="0" width={bw} height={h} />;
    x += bw + gap;
    return r;
  });
  return <svg width={w} height={h} viewBox={`0 0 ${x} ${h}`} preserveAspectRatio="none" fill={color} aria-hidden="true">{rects}</svg>;
}

const PATHS: Record<string, JSX.Element> = {
  wallet: <><rect x="3" y="6" width="18" height="13" {...S} /><path d="M3 9h18" {...S} /><path d="M16 13h2" {...S} /></>,
  activity: <path d="M3 12h3l2.5-7 4 14 3-9 1.5 2H21" {...S} />,
  identity: <><circle cx="12" cy="8.5" r="3.6" {...S} /><path d="M5 20c0-3.6 3.1-6 7-6s7 2.4 7 6" {...S} /></>,
  scan: <><path d="M4 8V5a1 1 0 0 1 1-1h3M16 4h3a1 1 0 0 1 1 1v3M20 16v3a1 1 0 0 1-1 1h-3M8 20H5a1 1 0 0 1-1-1v-3" {...S} /><path d="M4 12h16" {...S} /></>,
  shield: <path d="M12 3l7 2.5v5c0 4.4-3 8-7 10.5-4-2.5-7-6.1-7-10.5v-5L12 3z" {...S} />,
  shieldCheck: <><path d="M12 3l7 2.5v5c0 4.4-3 8-7 10.5-4-2.5-7-6.1-7-10.5v-5L12 3z" {...S} /><path d="M9 11.8l2.2 2.2L15 10.2" {...S} /></>,
  fingerprint: <><path d="M12 4.6c-3.6 0-6.5 2.9-6.5 6.5" {...S} /><path d="M18.5 11.5c0-3-2-5.2-4.4-6" {...S} /><path d="M8.6 11.1a3.4 3.4 0 0 1 6.8 0c0 3-.4 5.6-1.3 7.7" {...S} /><path d="M12 11.1v3.2c0 1.8-.3 3.4-.9 5" {...S} /><path d="M5.6 14.4c.3 1.6.3 3.3-.2 5" {...S} /></>,
  lock: <><rect x="5" y="10" width="14" height="10" {...S} /><path d="M8 10V7.5a4 4 0 0 1 8 0V10" {...S} /><path d="M12 14v2.5" {...S} /></>,
  qr: <><rect x="4" y="4" width="6" height="6" {...S} /><rect x="14" y="4" width="6" height="6" {...S} /><rect x="4" y="14" width="6" height="6" {...S} /><path d="M14 14h2v2M20 14v2M14 18v2h2M18 18v2h2" {...S} /></>,
  close: <path d="M6 6l12 12M18 6L6 18" {...S} />,
  plus: <path d="M12 5v14M5 12h14" {...S} />,
  check: <path d="M5 12.5l4.5 4.5L19 7" {...S} />,
  chevR: <path d="M9 5l7 7-7 7" {...S} />,
  chevL: <path d="M15 5l-7 7 7 7" {...S} />,
  chevD: <path d="M5 9l7 7 7-7" {...S} />,
  share: <><path d="M12 15V4" {...S} /><path d="M8 8l4-4 4 4" {...S} /><path d="M5 13v6a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-6" {...S} /></>,
  arrowUR: <><path d="M7 17L17 7" {...S} /><path d="M9 7h8v8" {...S} /></>,
  eye: <><path d="M2.5 12S6 5.5 12 5.5 21.5 12 21.5 12 18 18.5 12 18.5 2.5 12 2.5 12z" {...S} /><circle cx="12" cy="12" r="3" {...S} /></>,
  copy: <><rect x="8" y="8" width="11" height="11" {...S} /><path d="M5 16V5h11" {...S} /></>,
  bell: <><path d="M6 9a6 6 0 0 1 12 0c0 5 1.5 6.5 1.5 6.5H4.5S6 14 6 9z" {...S} /><path d="M10 19a2 2 0 0 0 4 0" {...S} /></>,
  gear: <><circle cx="12" cy="12" r="3" {...S} /><path d="M12 2.5v2.5M12 19v2.5M2.5 12H5M19 12h2.5M5.2 5.2l1.8 1.8M17 17l1.8 1.8M18.8 5.2L17 7M7 17l-1.8 1.8" {...S} /></>,
  info: <><circle cx="12" cy="12" r="8.5" {...S} /><path d="M12 11v5" {...S} /><circle cx="12" cy="8" r="0.6" fill="currentColor" stroke="none" /></>,
  time: <><circle cx="12" cy="12" r="8.5" {...S} /><path d="M12 7.5V12l3 2" {...S} /></>,
  key: <><circle cx="8" cy="12" r="3.5" {...S} /><path d="M11.3 11h8.2M16.5 11v3M19.5 11v2.5" {...S} /></>,
  link: <><path d="M10 14a4 4 0 0 0 5.6 0l2.4-2.4a4 4 0 0 0-5.6-5.6L11 7.4" {...S} /><path d="M14 10a4 4 0 0 0-5.6 0L6 12.4a4 4 0 0 0 5.6 5.6L13 16.6" {...S} /></>,
  doc: <><path d="M7 3h7l4 4v14H7z" {...S} /><path d="M14 3v4h4" {...S} /><path d="M10 13h6M10 16.5h6" {...S} /></>,
  cert: <><circle cx="12" cy="10" r="5.5" {...S} /><path d="M9 14.5L8 21l4-2 4 2-1-6.5" {...S} /></>,
  globe: <><circle cx="12" cy="12" r="8.5" {...S} /><path d="M3.5 12h17M12 3.5c2.4 2.3 3.5 5.3 3.5 8.5s-1.1 6.2-3.5 8.5c-2.4-2.3-3.5-5.3-3.5-8.5S9.6 5.8 12 3.5z" {...S} /></>,
  cal: <><rect x="4" y="5" width="16" height="15" {...S} /><path d="M4 9h16M8 3v4M16 3v4" {...S} /></>,
  building: <><rect x="6" y="3" width="12" height="18" {...S} /><path d="M9.5 7h2M12.5 7h2M9.5 10.5h2M12.5 10.5h2M9.5 14h2M12.5 14h2M10 21v-3h4v3" {...S} /></>,
  car: <><path d="M5 16v2M19 16v2M4 14l1.5-5h13L20 14v3H4z" {...S} /><path d="M4 14h16" {...S} /><circle cx="7.5" cy="16.5" r="0.4" fill="currentColor" stroke="none" /></>,
  user: <><circle cx="12" cy="8" r="4" {...S} /><path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" {...S} /></>,
};

export function Icon({ name, size = 22, color = "currentColor", strokeWidth }: { name: string; size?: number; color?: string; strokeWidth?: number }) {
  const node = PATHS[name] || PATHS.info;
  const extra = strokeWidth ? { strokeWidth } : {};
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" color={color} aria-hidden="true" style={{ display: "block", ...extra }}>
      {node}
    </svg>
  );
}
