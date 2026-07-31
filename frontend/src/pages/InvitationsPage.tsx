import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface RoleResponse {
  id: string;
  code: string;
  name: string;
}

interface InvitationResponse {
  id: string;
  email: string;
  roleId: string;
  status: "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";
  expiresAt: string;
  createdAt: string;
}

export function InvitationsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [invitations, setInvitations] = useState<InvitationResponse[]>([]);
  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [email, setEmail] = useState("");
  const [roleId, setRoleId] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client.get<InvitationResponse[]>("/api/v1/invitations").then((r) => setInvitations(r.data)).catch(() => setError("Could not load invitations - requires PLATFORM_ADMIN or ORG_ADMIN."));
    client.get<RoleResponse[]>("/api/v1/roles").then((r) => setRoles(r.data)).catch(() => {});
  }

  useEffect(load, [tenantId]);

  async function invite() {
    setError(null);
    try {
      await client.post("/api/v1/invitations", { email, roleId });
      setEmail("");
      load();
    } catch {
      setError("Could not create invitation.");
    }
  }

  async function revoke(id: string) {
    setError(null);
    try {
      await client.post(`/api/v1/invitations/${id}/revoke`);
      load();
    } catch {
      setError("Could not revoke invitation.");
    }
  }

  function roleName(id: string): string {
    return roles.find((r) => r.id === id)?.name ?? id;
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage invitations.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Invitations</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <input
          className="border rounded px-2 py-1"
          placeholder="Email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <select className="border rounded px-2 py-1" value={roleId} onChange={(e) => setRoleId(e.target.value)}>
          <option value="">Select role…</option>
          {roles.map((role) => (
            <option key={role.id} value={role.id}>
              {role.name}
            </option>
          ))}
        </select>
        <button className="btn-primary" onClick={invite} disabled={!email || !roleId}>
          Invite
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Email</th>
            <th className="p-2">Role</th>
            <th className="p-2">Status</th>
            <th className="p-2">Expires</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {invitations.map((invitation) => (
            <tr key={invitation.id} className="border-b text-sm">
              <td className="p-2">{invitation.email}</td>
              <td className="p-2">{roleName(invitation.roleId)}</td>
              <td className="p-2">{invitation.status}</td>
              <td className="p-2">{new Date(invitation.expiresAt).toLocaleString()}</td>
              <td className="p-2">
                {invitation.status === "PENDING" && (
                  <button className="text-red-600" onClick={() => revoke(invitation.id)}>
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
