import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";

const CLAIM_TYPES = ["STRING", "NUMBER", "BOOLEAN", "DATE"] as const;

interface ClaimDefinition {
  name: string;
  type: typeof CLAIM_TYPES[number];
  required: boolean;
}

interface SchemaResponse {
  id: string;
  name: string;
  version: string;
  format: "W3C_VC" | "SD_JWT_VC";
  status: "DRAFT" | "PUBLISHED" | "DEPRECATED";
  claims: ClaimDefinition[];
  createdAt: string;
}

/**
 * Full credential schema lifecycle (create as DRAFT -> publish -> deprecate, plus new versions).
 * The Credentials page (src/pages/CredentialsPage.tsx) only reads published schemas to issue
 * against - this page is where they're authored.
 */
export function SchemasPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [name, setName] = useState("");
  const [version, setVersion] = useState("1.0.0");
  const [claims, setClaims] = useState<ClaimDefinition[]>([{ name: "", type: "STRING", required: true }]);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<PageResponse<SchemaResponse>>("/api/v1/credential-schemas", { params: { size: 100 } })
      .then((r) => setSchemas(r.data.content))
      .catch(() => setError("Could not load credential schemas."));
  }

  useEffect(load, [tenantId]);

  function updateClaim(index: number, patch: Partial<ClaimDefinition>) {
    setClaims((prev) => prev.map((c, i) => (i === index ? { ...c, ...patch } : c)));
  }

  function addClaim() {
    setClaims((prev) => [...prev, { name: "", type: "STRING", required: true }]);
  }

  function removeClaim(index: number) {
    setClaims((prev) => prev.filter((_, i) => i !== index));
  }

  async function create() {
    setError(null);
    try {
      await client.post("/api/v1/credential-schemas", {
        name,
        version,
        format: "W3C_VC",
        claims: claims.filter((c) => c.name),
      });
      setName("");
      setVersion("1.0.0");
      setClaims([{ name: "", type: "STRING", required: true }]);
      load();
    } catch {
      setError("Could not create schema - requires PLATFORM_ADMIN, ORG_ADMIN, or ISSUER.");
    }
  }

  async function publish(id: string) {
    setError(null);
    try {
      await client.post(`/api/v1/credential-schemas/${id}/publish`);
      load();
    } catch {
      setError("Could not publish schema.");
    }
  }

  async function deprecate(id: string) {
    setError(null);
    try {
      await client.post(`/api/v1/credential-schemas/${id}/deprecate`);
      load();
    } catch {
      setError("Could not deprecate schema.");
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage credential schemas.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Credential Schemas</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="bg-white shadow rounded p-4 mb-6 max-w-2xl">
        <div className="flex gap-2 mb-2">
          <input className="border rounded px-2 py-1" placeholder="Schema name" value={name} onChange={(e) => setName(e.target.value)} />
          <input className="border rounded px-2 py-1 w-28" placeholder="Version" value={version} onChange={(e) => setVersion(e.target.value)} />
        </div>
        <p className="text-sm text-gray-500 mb-1">Claims (W3C VC format only)</p>
        {claims.map((c, i) => (
          <div key={i} className="flex gap-2 mb-1 items-center">
            <input
              className="border rounded px-2 py-1"
              placeholder="claim name"
              value={c.name}
              onChange={(e) => updateClaim(i, { name: e.target.value })}
            />
            <select className="border rounded px-2 py-1" value={c.type} onChange={(e) => updateClaim(i, { type: e.target.value as ClaimDefinition["type"] })}>
              {CLAIM_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
            <label className="text-xs flex items-center gap-1">
              <input type="checkbox" checked={c.required} onChange={(e) => updateClaim(i, { required: e.target.checked })} />
              required
            </label>
            <button className="text-red-600 text-xs" onClick={() => removeClaim(i)}>
              remove
            </button>
          </div>
        ))}
        <button className="text-blue-600 text-sm mb-2" onClick={addClaim}>
          + add claim
        </button>
        <div>
          <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={create} disabled={!name}>
            Create schema (DRAFT)
          </button>
        </div>
      </div>

      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Version</th>
            <th className="p-2">Claims</th>
            <th className="p-2">Status</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {schemas.map((schema) => (
            <tr key={schema.id} className="border-b text-sm">
              <td className="p-2">{schema.name}</td>
              <td className="p-2">{schema.version}</td>
              <td className="p-2 text-xs text-gray-500">{schema.claims.map((c) => c.name).join(", ")}</td>
              <td className="p-2">{schema.status}</td>
              <td className="p-2">
                {schema.status === "DRAFT" && (
                  <button className="text-green-600" onClick={() => publish(schema.id)}>
                    Publish
                  </button>
                )}
                {schema.status === "PUBLISHED" && (
                  <button className="text-yellow-600" onClick={() => deprecate(schema.id)}>
                    Deprecate
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
