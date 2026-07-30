import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import type { PageResponse } from "../lib/pageResponse";

interface OrganizationResponse {
  id: string;
  name: string;
  description: string | null;
  status: "ACTIVE" | "INACTIVE";
  contactEmail: string | null;
  createdAt: string;
}

/**
 * Platform-wide organization administration (PLATFORM_ADMIN/ORG_ADMIN). Not tenant-scoped - no
 * X-Tenant-ID header required (see identity.organization.OrganizationController's Javadoc).
 */
export function OrganizationsPage() {
  const auth = useAuth();
  const client = createApiClient(auth.user?.access_token, null);

  const [organizations, setOrganizations] = useState<OrganizationResponse[]>([]);
  const [name, setName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    client
      .get<PageResponse<OrganizationResponse>>("/api/v1/organizations")
      .then((r) => setOrganizations(r.data.content))
      .catch(() => setError("Could not load organizations - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, []);

  async function create() {
    setError(null);
    try {
      await client.post("/api/v1/organizations", { name, contactEmail: contactEmail || null });
      setName("");
      setContactEmail("");
      load();
    } catch {
      setError("Could not create organization - requires PLATFORM_ADMIN.");
    }
  }

  async function deactivate(id: string) {
    setError(null);
    try {
      await client.post(`/api/v1/organizations/${id}/deactivate`);
      load();
    } catch {
      setError("Could not deactivate organization - requires PLATFORM_ADMIN.");
    }
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Organizations</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <input
          className="border rounded px-2 py-1"
          placeholder="Organization name"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
        <input
          className="border rounded px-2 py-1"
          placeholder="Contact email (optional)"
          value={contactEmail}
          onChange={(e) => setContactEmail(e.target.value)}
        />
        <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={create} disabled={!name}>
          Create organization
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Contact</th>
            <th className="p-2">Status</th>
            <th className="p-2">Created</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {organizations.map((org) => (
            <tr key={org.id} className="border-b text-sm">
              <td className="p-2">{org.name}</td>
              <td className="p-2">{org.contactEmail ?? "—"}</td>
              <td className="p-2">{org.status}</td>
              <td className="p-2">{new Date(org.createdAt).toLocaleString()}</td>
              <td className="p-2">
                {org.status === "ACTIVE" && (
                  <button className="text-red-600" onClick={() => deactivate(org.id)}>
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
