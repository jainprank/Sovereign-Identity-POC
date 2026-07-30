import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface AuditRecordResponse {
  id: string;
  actorUserId: string | null;
  action: string;
  resourceType: string;
  resourceId: string;
  result: string;
  occurredAt: string;
}

/** Audit trail viewer + CSV export (FR-AUD-009/010, E15). */
export function AuditLogPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [records, setRecords] = useState<AuditRecordResponse[]>([]);
  const [action, setAction] = useState("");
  const [resourceType, setResourceType] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<AuditRecordResponse[]>("/api/v1/audit-records", {
        params: { action: action || undefined, resourceType: resourceType || undefined, size: 100 },
      })
      .then((r) => setRecords(r.data))
      .catch(() => setError("Could not load audit records - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, [tenantId]);

  async function exportCsv() {
    setError(null);
    try {
      const response = await client.get("/api/v1/audit-records/export", {
        params: { action: action || undefined, resourceType: resourceType || undefined },
        responseType: "blob",
      });
      const url = URL.createObjectURL(response.data as Blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "audit-export.csv";
      link.click();
      URL.revokeObjectURL(url);
    } catch {
      setError("Could not export audit records.");
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to view the audit log.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Audit Log</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <input className="border rounded px-2 py-1" placeholder="Filter by action" value={action} onChange={(e) => setAction(e.target.value)} />
        <input className="border rounded px-2 py-1" placeholder="Filter by resource type" value={resourceType} onChange={(e) => setResourceType(e.target.value)} />
        <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={load}>
          Search
        </button>
        <button className="border rounded px-4 py-1" onClick={exportCsv}>
          Export CSV
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Action</th>
            <th className="p-2">Resource</th>
            <th className="p-2">Result</th>
            <th className="p-2">Actor</th>
            <th className="p-2">When</th>
          </tr>
        </thead>
        <tbody>
          {records.map((record) => (
            <tr key={record.id} className="border-b text-sm">
              <td className="p-2">{record.action}</td>
              <td className="p-2 text-xs">{record.resourceType}/{record.resourceId}</td>
              <td className={"p-2 " + (record.result === "FAILURE" ? "text-red-600" : "text-gray-800")}>{record.result}</td>
              <td className="p-2 text-xs font-mono">{record.actorUserId ?? "system"}</td>
              <td className="p-2">{new Date(record.occurredAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
