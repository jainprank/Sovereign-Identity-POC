import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface ApiRequestLogResponse {
  id: string;
  apiClientId: string;
  method: string;
  path: string;
  statusCode: number;
  durationMs: number;
  correlationId: string;
  createdAt: string;
}

export function ApiLogsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [logs, setLogs] = useState<ApiRequestLogResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<ApiRequestLogResponse[]>("/api/v1/api-logs", { params: { page: 0, size: 100 } })
      .then((r) => setLogs(r.data))
      .catch(() => setError("Could not load API request logs - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, [tenantId]);

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to view API request logs.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">API Request Logs</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Method</th>
            <th className="p-2">Path</th>
            <th className="p-2">Status</th>
            <th className="p-2">Duration</th>
            <th className="p-2">Correlation ID</th>
            <th className="p-2">When</th>
          </tr>
        </thead>
        <tbody>
          {logs.map((log) => (
            <tr key={log.id} className="border-b text-sm">
              <td className="p-2 font-mono">{log.method}</td>
              <td className="p-2 font-mono text-xs">{log.path}</td>
              <td className={"p-2 " + (log.statusCode >= 400 ? "text-red-600" : "text-gray-800")}>{log.statusCode}</td>
              <td className="p-2">{log.durationMs}ms</td>
              <td className="p-2 text-xs">{log.correlationId}</td>
              <td className="p-2">{new Date(log.createdAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
