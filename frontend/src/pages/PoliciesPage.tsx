import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface PolicyRules {
  schemaId: string | null;
  requiredClaims: string[] | null;
  trustedIssuerDids: string[] | null;
  requireActiveStatus: boolean;
  requireNotExpired: boolean;
}

interface PolicyResponse {
  id: string;
  name: string;
  version: number;
  rules: PolicyRules;
  status: "DRAFT" | "ACTIVE" | "INACTIVE";
  createdAt: string;
}

/** Verification policy authoring - the rule sets an OID4VP verification request can be checked against. */
export function PoliciesPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [name, setName] = useState("");
  const [requiredClaims, setRequiredClaims] = useState("");
  const [trustedIssuerDids, setTrustedIssuerDids] = useState("");
  const [requireActiveStatus, setRequireActiveStatus] = useState(true);
  const [requireNotExpired, setRequireNotExpired] = useState(true);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<PolicyResponse[]>("/api/v1/verification-policies")
      .then((r) => setPolicies(r.data))
      .catch(() => setError("Could not load verification policies - requires PLATFORM_ADMIN, ORG_ADMIN, or VERIFIER."));
  }

  useEffect(load, [tenantId]);

  async function create() {
    setError(null);
    try {
      await client.post("/api/v1/verification-policies", {
        name,
        rules: {
          requiredClaims: requiredClaims ? requiredClaims.split(",").map((c) => c.trim()).filter(Boolean) : null,
          trustedIssuerDids: trustedIssuerDids ? trustedIssuerDids.split(",").map((c) => c.trim()).filter(Boolean) : null,
          requireActiveStatus,
          requireNotExpired,
        },
      });
      setName("");
      setRequiredClaims("");
      setTrustedIssuerDids("");
      load();
    } catch {
      setError("Could not create policy.");
    }
  }

  async function setStatus(id: string, action: "activate" | "deactivate") {
    setError(null);
    try {
      await client.post(`/api/v1/verification-policies/${id}/${action}`);
      load();
    } catch {
      setError(`Could not ${action} policy.`);
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage verification policies.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Verification Policies</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="bg-white shadow rounded p-4 mb-6 grid grid-cols-2 gap-3 max-w-2xl">
        <input className="border rounded px-2 py-1 col-span-2" placeholder="Policy name" value={name} onChange={(e) => setName(e.target.value)} />
        <input
          className="border rounded px-2 py-1"
          placeholder="Required claims (comma-separated)"
          value={requiredClaims}
          onChange={(e) => setRequiredClaims(e.target.value)}
        />
        <input
          className="border rounded px-2 py-1"
          placeholder="Trusted issuer DIDs (comma-separated)"
          value={trustedIssuerDids}
          onChange={(e) => setTrustedIssuerDids(e.target.value)}
        />
        <label className="text-sm flex items-center gap-1">
          <input type="checkbox" checked={requireActiveStatus} onChange={(e) => setRequireActiveStatus(e.target.checked)} />
          require active status
        </label>
        <label className="text-sm flex items-center gap-1">
          <input type="checkbox" checked={requireNotExpired} onChange={(e) => setRequireNotExpired(e.target.checked)} />
          require not expired
        </label>
        <button className="bg-blue-600 text-white rounded px-4 py-1 col-span-2" onClick={create} disabled={!name}>
          Create policy (DRAFT)
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Version</th>
            <th className="p-2">Required claims</th>
            <th className="p-2">Status</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {policies.map((policy) => (
            <tr key={policy.id} className="border-b text-sm">
              <td className="p-2">{policy.name}</td>
              <td className="p-2">{policy.version}</td>
              <td className="p-2 text-xs text-gray-500">{policy.rules.requiredClaims?.join(", ") ?? "—"}</td>
              <td className="p-2">{policy.status}</td>
              <td className="p-2">
                {policy.status !== "ACTIVE" && (
                  <button className="text-green-600" onClick={() => setStatus(policy.id, "activate")}>
                    Activate
                  </button>
                )}
                {policy.status === "ACTIVE" && (
                  <button className="text-yellow-600" onClick={() => setStatus(policy.id, "deactivate")}>
                    Deactivate
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
