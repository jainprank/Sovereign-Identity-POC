import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

export function DashboardPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const [unreadCount, setUnreadCount] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!tenantId) {
      return;
    }
    const client = createApiClient(auth.user?.access_token, tenantId);
    client
      .get("/api/v1/notifications/unread-count")
      .then((response) => setUnreadCount(response.data.unread))
      .catch(() => setError("Could not load unread notification count."));
  }, [auth.user?.access_token, tenantId]);

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to load your dashboard.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Dashboard</h2>
      {error && <p className="text-red-600">{error}</p>}
      <div className="bg-white shadow rounded p-4 w-64">
        <p className="text-sm text-gray-500">Unread notifications</p>
        <p className="text-3xl font-semibold">{unreadCount ?? "…"}</p>
      </div>
    </div>
  );
}
