import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface SchemaResponse {
  id: string;
  name: string;
  version: string;
  status: string;
}

interface CredentialResponse {
  id: string;
  subjectDid: string;
  status: string;
  issuedAt: string;
}

export function CredentialsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [credentials, setCredentials] = useState<CredentialResponse[]>([]);
  const [schemaId, setSchemaId] = useState("");
  const [issuerDid, setIssuerDid] = useState("");
  const [subjectDid, setSubjectDid] = useState("");
  const [claimsJson, setClaimsJson] = useState('{\n  "name": "Alice"\n}');
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client.get<SchemaResponse[]>("/api/v1/credential-schemas").then((r) => setSchemas(r.data)).catch(() => {});
    client.get<CredentialResponse[]>("/api/v1/credentials").then((r) => setCredentials(r.data)).catch(() => {});
  }

  useEffect(load, [tenantId]);

  async function issue() {
    setError(null);
    let claims: Record<string, unknown>;
    try {
      claims = JSON.parse(claimsJson);
    } catch {
      setError("Claims must be valid JSON.");
      return;
    }
    try {
      await client.post("/api/v1/credentials", { schemaId, issuerDid, subjectDid, claims });
      load();
    } catch {
      setError("Could not issue credential - check the schema is PUBLISHED and the issuer DID is valid.");
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage credentials.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Credentials</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="bg-white shadow rounded p-4 mb-6 grid grid-cols-2 gap-3 max-w-2xl">
        <select className="border rounded px-2 py-1 col-span-2" value={schemaId} onChange={(e) => setSchemaId(e.target.value)}>
          <option value="">Select schema…</option>
          {schemas.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} v{s.version} ({s.status})
            </option>
          ))}
        </select>
        <input
          className="border rounded px-2 py-1"
          placeholder="Issuer DID"
          value={issuerDid}
          onChange={(e) => setIssuerDid(e.target.value)}
        />
        <input
          className="border rounded px-2 py-1"
          placeholder="Subject DID (optional)"
          value={subjectDid}
          onChange={(e) => setSubjectDid(e.target.value)}
        />
        <textarea
          className="border rounded px-2 py-1 col-span-2 font-mono text-sm"
          rows={5}
          value={claimsJson}
          onChange={(e) => setClaimsJson(e.target.value)}
        />
        <button className="bg-blue-600 text-white rounded px-4 py-1 col-span-2" onClick={issue} disabled={!schemaId || !issuerDid}>
          Issue credential
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Subject DID</th>
            <th className="p-2">Status</th>
            <th className="p-2">Issued</th>
          </tr>
        </thead>
        <tbody>
          {credentials.map((c) => (
            <tr key={c.id} className="border-b text-sm">
              <td className="p-2 font-mono">{c.subjectDid ?? "—"}</td>
              <td className="p-2">{c.status}</td>
              <td className="p-2">{new Date(c.issuedAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
