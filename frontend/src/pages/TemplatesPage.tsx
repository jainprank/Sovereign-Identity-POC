import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";

interface SchemaResponse {
  id: string;
  name: string;
  version: string;
  status: string;
}

interface TemplateResponse {
  id: string;
  schemaId: string;
  name: string;
  description: string | null;
  defaultExpiresInDays: number | null;
  createdAt: string;
}

/** Reusable issuance presets (default claims/expiry) built on top of a credential schema. */
export function TemplatesPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [templates, setTemplates] = useState<TemplateResponse[]>([]);
  const [schemaId, setSchemaId] = useState("");
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const [defaultExpiresInDays, setDefaultExpiresInDays] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<PageResponse<SchemaResponse>>("/api/v1/credential-schemas", { params: { size: 100 } })
      .then((r) => setSchemas(r.data.content))
      .catch(() => {});
    client
      .get<PageResponse<TemplateResponse>>("/api/v1/credential-templates")
      .then((r) => setTemplates(r.data.content))
      .catch(() => setError("Could not load credential templates."));
  }

  useEffect(load, [tenantId]);

  async function create() {
    setError(null);
    try {
      await client.post("/api/v1/credential-templates", {
        schemaId,
        name,
        description: description || null,
        defaultExpiresInDays: defaultExpiresInDays ? Number(defaultExpiresInDays) : null,
      });
      setName("");
      setDescription("");
      setDefaultExpiresInDays("");
      load();
    } catch {
      setError("Could not create template - requires PLATFORM_ADMIN, ORG_ADMIN, or ISSUER.");
    }
  }

  async function remove(id: string) {
    setError(null);
    try {
      await client.delete(`/api/v1/credential-templates/${id}`);
      load();
    } catch {
      setError("Could not delete template.");
    }
  }

  function schemaLabel(id: string): string {
    const schema = schemas.find((s) => s.id === id);
    return schema ? `${schema.name} v${schema.version}` : id;
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage credential templates.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Credential Templates</h2>
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
        <input className="border rounded px-2 py-1" placeholder="Template name" value={name} onChange={(e) => setName(e.target.value)} />
        <input
          className="border rounded px-2 py-1"
          placeholder="Default expiry (days, optional)"
          value={defaultExpiresInDays}
          onChange={(e) => setDefaultExpiresInDays(e.target.value)}
        />
        <input
          className="border rounded px-2 py-1 col-span-2"
          placeholder="Description (optional)"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <button className="bg-blue-600 text-white rounded px-4 py-1 col-span-2" onClick={create} disabled={!schemaId || !name}>
          Create template
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Schema</th>
            <th className="p-2">Default expiry</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {templates.map((t) => (
            <tr key={t.id} className="border-b text-sm">
              <td className="p-2">{t.name}</td>
              <td className="p-2">{schemaLabel(t.schemaId)}</td>
              <td className="p-2">{t.defaultExpiresInDays ? `${t.defaultExpiresInDays} days` : "—"}</td>
              <td className="p-2">
                <button className="text-red-600" onClick={() => remove(t.id)}>
                  Delete
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
