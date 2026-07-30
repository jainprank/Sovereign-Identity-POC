import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";

interface UserResponse {
  id: string;
  email: string;
  displayName: string;
  status: "INVITED" | "ACTIVE" | "SUSPENDED" | "DEACTIVATED";
  createdAt: string;
}

export function UsersPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [users, setUsers] = useState<UserResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<PageResponse<UserResponse>>("/api/v1/users")
      .then((r) => setUsers(r.data.content))
      .catch(() => setError("Could not load users - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, [tenantId]);

  async function setStatus(id: string, action: "suspend" | "reactivate" | "deactivate") {
    setError(null);
    try {
      await client.post(`/api/v1/users/${id}/${action}`);
      load();
    } catch {
      setError(`Could not ${action} user.`);
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage users.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Users</h2>
      <p className="text-sm text-gray-500 mb-4">
        Users are created via invitation acceptance, not directly here - see the Invitations page.
      </p>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Email</th>
            <th className="p-2">Status</th>
            <th className="p-2">Created</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <tr key={user.id} className="border-b text-sm">
              <td className="p-2">{user.displayName}</td>
              <td className="p-2">{user.email}</td>
              <td className="p-2">{user.status}</td>
              <td className="p-2">{new Date(user.createdAt).toLocaleString()}</td>
              <td className="p-2 flex gap-2">
                {user.status === "SUSPENDED" && (
                  <button className="text-green-600" onClick={() => setStatus(user.id, "reactivate")}>
                    Reactivate
                  </button>
                )}
                {user.status === "ACTIVE" && (
                  <button className="text-yellow-600" onClick={() => setStatus(user.id, "suspend")}>
                    Suspend
                  </button>
                )}
                {user.status !== "DEACTIVATED" && (
                  <button className="text-red-600" onClick={() => setStatus(user.id, "deactivate")}>
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
