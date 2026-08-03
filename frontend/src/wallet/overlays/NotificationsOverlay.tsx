import { useEffect } from "react";
import type { AxiosInstance } from "axios";
import type { NotificationResponse } from "../api/types";
import { markNotificationRead } from "../api/notificationApi";
import { Icon } from "../components/icons";
import { formatDateTime } from "../lib/credentialDisplay";

function iconFor(eventType: string): { icon: string; cls: string } {
  if (eventType.startsWith("Credential")) return { icon: "doc", cls: "ico-issue" };
  if (eventType.startsWith("Verification")) return { icon: "shieldCheck", cls: "ico-verify" };
  return { icon: "bell", cls: "ico-revoke" };
}

interface Props {
  api: AxiosInstance;
  notifications: NotificationResponse[];
  loading: boolean;
  error: string | null;
  onClose: () => void;
  onRead: (id: string) => void;
}

/** Real feed from GET /api/v1/notifications (EPIC 17) — see notificationApi.ts for the self-service, no-role-required scope. */
export function NotificationsOverlay({ api, notifications, loading, error, onClose, onRead }: Props) {
  useEffect(() => {
    const unread = notifications.filter((n) => !n.read).map((n) => n.id);
    unread.forEach((id) => {
      markNotificationRead(api, id).then(() => onRead(id)).catch(() => {});
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [notifications.length]);

  return (
    <div className="idt-overlay">
      <div className="idt-ovheader">
        <button className="idt-iconbtn" aria-label="Close" onClick={onClose}><Icon name="close" size={18} /></button>
        <div className="idt-ovtitle">Notifications</div>
        <div style={{ width: 40 }} />
      </div>
      <div className="idt-view">
        {loading && <div className="idt-loading">Loading notifications…</div>}
        {error && <div className="idt-error">{error}</div>}
        {!loading && !error && (
          <div className="idt-timeline">
            {notifications.length === 0 && <div className="idt-empty">No notifications yet.</div>}
            {notifications.map((n) => {
              const meta = iconFor(n.eventType);
              return (
                <div className="idt-event" key={n.id}>
                  <div className="idt-event__rail">
                    <div className={`idt-event__ico ${meta.cls}`}><Icon name={meta.icon} size={17} /></div>
                    <div className="idt-event__line" />
                  </div>
                  <div className="idt-event__body">
                    <div className="idt-event__title">{n.message}</div>
                    <div className="idt-event__time"><Icon name="time" size={12} />{formatDateTime(n.createdAt)}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
        <div className="idt-scroll-pad" />
      </div>
    </div>
  );
}
