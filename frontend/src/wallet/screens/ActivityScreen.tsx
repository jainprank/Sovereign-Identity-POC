import { useState } from "react";
import { AnchorRows } from "../components/AnchorBlock";
import { Icon } from "../components/icons";
import type { ActivityEvent } from "../uiTypes";
import { formatDateTime } from "../lib/credentialDisplay";

const FILTERS: Array<{ id: "all" | ActivityEvent["kind"]; label: string }> = [
  { id: "all", label: "All" },
  { id: "share", label: "Shared" },
  { id: "issue", label: "Issued" },
];

export function ActivityScreen({ events, loading, error }: { events: ActivityEvent[]; loading: boolean; error: string | null }) {
  const [filter, setFilter] = useState<"all" | ActivityEvent["kind"]>("all");
  const [open, setOpen] = useState<string | null>(null);
  const filtered = filter === "all" ? events : events.filter((e) => e.kind === filter);

  return (
    <div className="idt-view">
      <div className="idt-header">
        <h1 className="idt-title">Activity</h1>
      </div>
      <p style={{ padding: "0 20px 4px", color: "var(--muted)", fontSize: 13.5, margin: 0 }}>
        Every credential you receive and every attribute you share is recorded here as a signed, tamper-evident entry.
      </p>

      <div className="idt-filters">
        {FILTERS.map((f) => (
          <button key={f.id} className={`idt-fchip ${filter === f.id ? "on" : ""}`} onClick={() => setFilter(f.id)}>
            {f.label}
          </button>
        ))}
      </div>

      {loading && <div className="idt-loading">Loading your trust log…</div>}
      {error && <div className="idt-error">{error}</div>}

      {!loading && !error && (
        <div className="idt-timeline">
          {filtered.length === 0 && <div className="idt-empty">Nothing here yet.</div>}
          {filtered.map((e) => (
            <div className="idt-event" key={e.id}>
              <div className="idt-event__rail">
                <div className={`idt-event__ico ${e.cls}`}><Icon name={e.icon} size={17} /></div>
                <div className="idt-event__line" />
              </div>
              <button className="idt-event__body" onClick={() => setOpen(open === e.id ? null : e.id)} style={{ textAlign: "left", width: "100%" }}>
                <div className="idt-event__title">{e.title}</div>
                <div className="idt-event__desc">{e.desc}</div>
                <div className="idt-event__time"><Icon name="time" size={12} />{formatDateTime(e.time)}</div>
                {open === e.id && (
                  <div className="idt-event__expand">
                    <AnchorRows info={e.anchor} />
                  </div>
                )}
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="idt-scroll-pad" />
    </div>
  );
}
