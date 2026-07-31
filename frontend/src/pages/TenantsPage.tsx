import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import type { PageResponse } from "../lib/pageResponse";

interface OrganizationResponse {
  id: string;
  name: string;
}

interface TenantResponse {
  id: string;
  organizationId: string;
  name: string;
  status: "ACTIVE" | "SUSPENDED";
  createdAt: string;
}

/**
 * Platform-wide tenant administration. Not tenant-scoped itself (X-Tenant-ID isn't sent) - see
 * identity.tenant.TenantController, which manages tenants rather than operating within one.
 */
export function TenantsPage() {
  const auth = useAuth();
  const client = createApiClient(auth.user?.access_token, null);

  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [tenants, setTenants] = useState<TenantResponse[]>([]);
  const [organizationId, setOrganizationId] = useState("");
  const [name, setName] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    client
      .get<PageResponse<OrganizationResponse>>("/api/v1/organizations")
      .then((r) => setOrganizations(r.data.content))
      .catch(() => {});
    client
      .get<PageResponse<TenantResponse>>("/api/v1/tenants")
      .then((r) => setTenants(r.data.content))
      .catch(() => setError("Could not load tenants - requires PLATFORM_ADMIN."));
  }

  useEffect(load, []);

  async function create() {
    setError(null);
    try {
      await client.post(`/api/v1/organizations/${organizationId}/tenants`, { name });
      setName("");
      load();
    } catch {
      setError("Could not create tenant - requires PLATFORM_ADMIN, and a valid organization.");
    }
  }

  async function setStatus(id: string, action: "suspend" | "reactivate") {
    setError(null);
    try {
      await client.post(`/api/v1/tenants/${id}/${action}`);
      load();
    } catch {
      setError(`Could not ${action} tenant.`);
    }
  }

  function orgName(organizationId: string): string {
    return organizations.find((o) => o.id === organizationId)?.name ?? organizationId;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Tenants</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <select
          className="border rounded px-2 py-1"
          value={organizationId}
          onChange={(e) => setOrganizationId(e.target.value)}
        >
          <option value="">Select organization…</option>
          {organizations.map((org) => (
            <option key={org.id} value={org.id}>
              {org.name}
            </option>
          ))}
        </select>
        <input
          className="border rounded px-2 py-1"
          placeholder="Tenant name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <button
          className="btn-primary"
          onClick={create}
          disabled={!organizationId || !name}
        >
          Create tenant
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Organization</th>
            <th className="p-2">Tenant ID</th>
            <th className="p-2">Status</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {tenants.map((tenant) => (
            <tr key={tenant.id} className="border-b text-sm">
              <td className="p-2">{tenant.name}</td>
              <td className="p-2">{orgName(tenant.organizationId)}</td>
              <td className="p-2 font-mono text-xs">{tenant.id}</td>
              <td className="p-2">{tenant.status}</td>
              <td className="p-2">
                {tenant.status === "ACTIVE" ? (
                  <button className="text-red-600" onClick={() => setStatus(tenant.id, "suspend")}>
                    Suspend
                  </button>
                ) : (
                  <button className="text-green-600" onClick={() => setStatus(tenant.id, "reactivate")}>
                    Reactivate
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
