import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface NotificationResponse {
  id: string;
  eventType: string;
  message: string;
  read: boolean;
  createdAt: string;
}

export function NotificationsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);
  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);

  function load() {
    if (!tenantId) return;
    client.get<NotificationResponse[]>("/api/v1/notifications").then((r) => setNotifications(r.data)).catch(() => {});
  }

  useEffect(load, [tenantId]);

  async function markRead(id: string) {
    await client.post(`/api/v1/notifications/${id}/read`);
    load();
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to view notifications.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Notifications</h2>
      <ul className="bg-white shadow rounded divide-y">
        {notifications.map((n) => (
          <li key={n.id} className="p-3 flex items-center justify-between">
            <div>
              <p className={n.read ? "text-gray-500" : "font-semibold"}>{n.message}</p>
              <p className="text-xs text-gray-400">
                {n.eventType} · {new Date(n.createdAt).toLocaleString()}
              </p>
            </div>
            {!n.read && (
              <button className="text-sm link-primary" onClick={() => markRead(n.id)}>
                Mark read
              </button>
            )}
          </li>
        ))}
        {notifications.length === 0 && <li className="p-3 text-gray-500">No notifications yet.</li>}
      </ul>
    </div>
  );
}
