import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface ApiClientResponse {
  id: string;
  name: string;
  clientType: "API_KEY" | "OAUTH_CLIENT";
  clientId: string;
  scopes: string[];
  status: "ACTIVE" | "REVOKED";
  lastUsedAt: string | null;
  createdAt: string;
}

export function ApiClientsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [clients, setClients] = useState<ApiClientResponse[]>([]);
  const [name, setName] = useState("");
  const [clientType, setClientType] = useState<"API_KEY" | "OAUTH_CLIENT">("API_KEY");
  const [scopes, setScopes] = useState("");
  const [newSecret, setNewSecret] = useState<{ clientId: string; secret: string } | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<ApiClientResponse[]>("/api/v1/api-clients")
      .then((r) => setClients(r.data))
      .catch(() => setError("Could not load API clients - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, [tenantId]);

  async function create() {
    setError(null);
    setNewSecret(null);
    try {
      const response = await client.post("/api/v1/api-clients", {
        name,
        clientType,
        scopes: scopes.split(",").map((s) => s.trim()).filter(Boolean),
      });
      setNewSecret({ clientId: response.data.client.clientId, secret: response.data.secret });
      setName("");
      setScopes("");
      load();
    } catch {
      setError("Could not create API client.");
    }
  }

  async function revoke(id: string) {
    setError(null);
    try {
      await client.post(`/api/v1/api-clients/${id}/revoke`);
      load();
    } catch {
      setError("Could not revoke API client.");
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage API clients.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">API Clients</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      {newSecret && (
        <div className="bg-yellow-50 border border-yellow-300 rounded p-3 mb-4 text-sm">
          <p className="font-semibold">Secret shown once - copy it now, it cannot be retrieved again:</p>
          <p className="font-mono break-all">
            client_id: {newSecret.clientId} / secret: {newSecret.secret}
          </p>
        </div>
      )}
      <div className="flex gap-2 mb-4">
        <input className="border rounded px-2 py-1" placeholder="Name" value={name} onChange={(e) => setName(e.target.value)} />
        <select className="border rounded px-2 py-1" value={clientType} onChange={(e) => setClientType(e.target.value as "API_KEY" | "OAUTH_CLIENT")}>
          <option value="API_KEY">API Key</option>
          <option value="OAUTH_CLIENT">OAuth Client</option>
        </select>
        <input
          className="border rounded px-2 py-1 w-64"
          placeholder="Scopes (comma-separated)"
          value={scopes}
          onChange={(e) => setScopes(e.target.value)}
        />
        <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={create} disabled={!name}>
          Create
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Type</th>
            <th className="p-2">Client ID</th>
            <th className="p-2">Scopes</th>
            <th className="p-2">Status</th>
            <th className="p-2">Last used</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {clients.map((c) => (
            <tr key={c.id} className="border-b text-sm">
              <td className="p-2">{c.name}</td>
              <td className="p-2">{c.clientType}</td>
              <td className="p-2 font-mono text-xs">{c.clientId}</td>
              <td className="p-2 text-xs">{c.scopes.join(", ") || "—"}</td>
              <td className="p-2">{c.status}</td>
              <td className="p-2">{c.lastUsedAt ? new Date(c.lastUsedAt).toLocaleString() : "never"}</td>
              <td className="p-2">
                {c.status === "ACTIVE" && (
                  <button className="text-red-600" onClick={() => revoke(c.id)}>
                    Revoke
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
