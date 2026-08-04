import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";
import { HederaAnchorLink } from "../components/HederaAnchorLink";

interface DidResponse {
  id: string;
  did: string;
  method: string;
  status: string;
  createdAt: string;
}

const METHODS = ["WEB", "KEY", "JWK"];

export function DidsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const [dids, setDids] = useState<DidResponse[]>([]);
  const [method, setMethod] = useState(METHODS[0]);
  const [domain, setDomain] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [created, setCreated] = useState<DidResponse | null>(null);

  const client = createApiClient(auth.user?.access_token, tenantId);

  function load() {
    if (!tenantId) return;
    client
      .get<PageResponse<DidResponse>>("/api/v1/dids")
      .then((response) => setDids(response.data.content))
      .catch(() => setError("Could not load DIDs."));
  }

  useEffect(load, [tenantId]);

  async function create() {
    setError(null);
    setCreating(true);
    try {
      const response = await client.post<DidResponse>("/api/v1/dids", { method, domain: domain || null, path: null });
      setDomain("");
      load();
      setCreated(response.data);
    } catch {
      setError("Could not create DID - check that your role has ISSUER/ORG_ADMIN/PLATFORM_ADMIN access.");
    } finally {
      setCreating(false);
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage DIDs.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">DIDs</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <select className="border rounded px-2 py-1" value={method} onChange={(e) => setMethod(e.target.value)}>
          {METHODS.map((m) => (
            <option key={m} value={m}>
              did:{m.toLowerCase()}
            </option>
          ))}
        </select>
        {method === "WEB" && (
          <input
            className="border rounded px-2 py-1"
            placeholder="domain (did:web only)"
            value={domain}
            onChange={(e) => setDomain(e.target.value)}
          />
        )}
        <button className="btn-primary" onClick={create} disabled={creating}>
          {creating ? (
            <span className="inline-flex items-center gap-2">
              <span
                className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"
                aria-hidden="true"
              />
              Creating DID…
            </span>
          ) : (
            "Create DID"
          )}
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">DID</th>
            <th className="p-2">Method</th>
            <th className="p-2">Status</th>
            <th className="p-2">Created</th>
            <th className="p-2">Hedera anchor</th>
          </tr>
        </thead>
        <tbody>
          {dids.map((did) => (
            <tr key={did.id} className="border-b text-sm">
              <td className="p-2 font-mono">{did.did}</td>
              <td className="p-2">{did.method}</td>
              <td className="p-2">{did.status}</td>
              <td className="p-2">{new Date(did.createdAt).toLocaleString()}</td>
              <td className="p-2">
                <HederaAnchorLink api={client} resourceType="DID" resourceId={did.id} />
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {created && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setCreated(null)}>
          <div className="bg-white rounded shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-green-100 text-green-600 text-lg">✓</span>
              <h3 className="text-lg font-semibold">DID created</h3>
            </div>
            <p className="text-sm text-gray-600 mb-3">
              The DID has been created and anchored to Hedera. It now appears in the table below.
            </p>
            <div className="bg-gray-50 rounded p-2 mb-4 font-mono text-xs break-all">{created.did}</div>
            <div className="flex items-center justify-between">
              <HederaAnchorLink api={client} resourceType="DID" resourceId={created.id} />
              <button className="btn-primary" onClick={() => setCreated(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
