import { Fragment, useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";
import { HederaAnchorLink } from "../components/HederaAnchorLink";

const CLAIM_TYPES = ["STRING", "NUMBER", "BOOLEAN", "DATE"] as const;

const EXAMPLE_VALUE_BY_TYPE: Record<typeof CLAIM_TYPES[number], unknown> = {
  STRING: "string",
  NUMBER: 0,
  BOOLEAN: true,
  DATE: "2026-01-01",
};

interface ClaimDefinition {
  name: string;
  type: typeof CLAIM_TYPES[number];
  required: boolean;
}

/** The exact claims JSON an issuer needs to paste into Issue & Browse's "Claims" field for this schema. */
function exampleClaimsJson(claims: ClaimDefinition[]): string {
  const example: Record<string, unknown> = {};
  for (const c of claims) {
    example[c.name] = EXAMPLE_VALUE_BY_TYPE[c.type];
  }
  return JSON.stringify(example, null, 2);
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
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [newVersionOf, setNewVersionOf] = useState<string | null>(null);

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

  function resetForm() {
    setName("");
    setVersion("1.0.0");
    setClaims([{ name: "", type: "STRING", required: true }]);
    setNewVersionOf(null);
  }

  async function create() {
    setError(null);
    try {
      if (newVersionOf) {
        await client.post(`/api/v1/credential-schemas/${newVersionOf}/versions`, {
          version,
          claims: claims.filter((c) => c.name),
        });
      } else {
        await client.post("/api/v1/credential-schemas", {
          name,
          version,
          format: "W3C_VC",
          claims: claims.filter((c) => c.name),
        });
      }
      resetForm();
      load();
    } catch {
      setError(
        newVersionOf
          ? "Could not create a new version of this schema."
          : "Could not create schema - requires PLATFORM_ADMIN, ORG_ADMIN, or ISSUER.",
      );
    }
  }

  /** PUBLISHED schemas are immutable (see CredentialSchemaService.requireDraft) - "editing" one
   * really means authoring a new version pre-filled with its current claims to tweak. */
  function startNewVersion(schema: SchemaResponse) {
    setNewVersionOf(schema.id);
    setName(schema.name);
    setVersion(bumpVersion(schema.version));
    setClaims(schema.claims.map((c) => ({ ...c })));
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function bumpVersion(v: string): string {
    const parts = v.split(".");
    const last = parseInt(parts[parts.length - 1], 10);
    if (Number.isNaN(last)) return `${v}.1`;
    parts[parts.length - 1] = String(last + 1);
    return parts.join(".");
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
        {newVersionOf && (
          <p className="text-sm text-indigo-600 mb-2">
            Creating a new version of "{name}" (published schemas can't be edited in place).{" "}
            <button className="underline" onClick={resetForm}>
              Cancel
            </button>
          </p>
        )}
        <div className="flex gap-2 mb-2">
          <input
            className="border rounded px-2 py-1"
            placeholder="Schema name"
            value={name}
            onChange={(e) => setName(e.target.value)}
            disabled={!!newVersionOf}
          />
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
        <button className="link-primary text-sm mb-2" onClick={addClaim}>
          + add claim
        </button>
        <div>
          <button className="btn-primary" onClick={create} disabled={!name}>
            {newVersionOf ? "Create new version (DRAFT)" : "Create schema (DRAFT)"}
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
            <th className="p-2">Hedera anchor</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {schemas.map((schema) => (
            <Fragment key={schema.id}>
              <tr className="border-b text-sm">
                <td className="p-2">{schema.name}</td>
                <td className="p-2">{schema.version}</td>
                <td className="p-2 text-xs text-gray-500">
                  {schema.claims.map((c) => c.name).join(", ")}{" "}
                  <button
                    className="text-indigo-600 hover:underline ml-1"
                    onClick={() => setExpandedId(expandedId === schema.id ? null : schema.id)}
                  >
                    {expandedId === schema.id ? "Hide JSON" : "View JSON"}
                  </button>
                </td>
                <td className="p-2">{schema.status}</td>
                <td className="p-2">
                  <HederaAnchorLink api={client} resourceType="CREDENTIAL_SCHEMA" resourceId={schema.id} />
                </td>
                <td className="p-2 whitespace-nowrap">
                  {schema.status === "DRAFT" && (
                    <button className="text-green-600" onClick={() => publish(schema.id)}>
                      Publish
                    </button>
                  )}
                  {schema.status !== "DRAFT" && (
                    <button className="text-indigo-600" onClick={() => startNewVersion(schema)}>
                      New version
                    </button>
                  )}
                  {schema.status === "PUBLISHED" && (
                    <button className="text-yellow-600 ml-2" onClick={() => deprecate(schema.id)}>
                      Deprecate
                    </button>
                  )}
                </td>
              </tr>
              {expandedId === schema.id && (
                <tr className="border-b bg-gray-50">
                  <td colSpan={6} className="p-3">
                    <div className="grid grid-cols-2 gap-4">
                      <div>
                        <p className="text-xs font-semibold text-gray-500 mb-1">Claim definitions</p>
                        <table className="text-xs w-full">
                          <thead>
                            <tr className="text-left text-gray-500">
                              <th className="pr-3">Name</th>
                              <th className="pr-3">Type</th>
                              <th>Required</th>
                            </tr>
                          </thead>
                          <tbody>
                            {schema.claims.map((c) => (
                              <tr key={c.name}>
                                <td className="pr-3 font-mono">{c.name}</td>
                                <td className="pr-3">{c.type}</td>
                                <td>{c.required ? "yes" : "no"}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      <div>
                        <p className="text-xs font-semibold text-gray-500 mb-1">
                          Expected claims JSON (paste into Issue &amp; Browse)
                        </p>
                        <pre className="bg-white border rounded p-2 text-xs font-mono overflow-x-auto">
                          {exampleClaimsJson(schema.claims)}
                        </pre>
                      </div>
                    </div>
                  </td>
                </tr>
              )}
            </Fragment>
          ))}
        </tbody>
      </table>
    </div>
  );
}
