import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";

interface PermissionResponse {
  id: string;
  resource: string;
  action: string;
  description: string;
}

interface RoleResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  system: boolean;
  permissions: PermissionResponse[];
}

interface UserResponse {
  id: string;
  email: string;
  displayName: string;
}

interface UserRoleResponse {
  id: string;
  userId: string;
  roleId: string;
  assignedAt: string;
}

export function RolesPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [roles, setRoles] = useState<RoleResponse[]>([]);
  const [permissions, setPermissions] = useState<PermissionResponse[]>([]);
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [assignments, setAssignments] = useState<UserRoleResponse[]>([]);

  const [code, setCode] = useState("");
  const [name, setName] = useState("");
  const [selectedPermissions, setSelectedPermissions] = useState<Set<string>>(new Set());
  const [assignUserId, setAssignUserId] = useState("");
  const [assignRoleId, setAssignRoleId] = useState("");
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client.get<RoleResponse[]>("/api/v1/roles").then((r) => setRoles(r.data)).catch(() => setError("Could not load roles."));
    client.get<PermissionResponse[]>("/api/v1/permissions").then((r) => setPermissions(r.data)).catch(() => {});
    client
      .get<PageResponse<UserResponse>>("/api/v1/users")
      .then((r) => setUsers(r.data.content))
      .catch(() => {});
  }

  useEffect(load, [tenantId]);

  useEffect(() => {
    if (!tenantId || !assignUserId) {
      setAssignments([]);
      return;
    }
    client
      .get<UserRoleResponse[]>(`/api/v1/roles/assignments/users/${assignUserId}`)
      .then((r) => setAssignments(r.data))
      .catch(() => setAssignments([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId, assignUserId]);

  function togglePermission(id: string) {
    setSelectedPermissions((prev) => {
      const next = new Set(prev);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  async function createRole() {
    setError(null);
    try {
      await client.post("/api/v1/roles", {
        code,
        name,
        permissionIds: Array.from(selectedPermissions),
      });
      setCode("");
      setName("");
      setSelectedPermissions(new Set());
      load();
    } catch {
      setError("Could not create role - code must be unique for this tenant.");
    }
  }

  async function assignRole() {
    setError(null);
    try {
      await client.post("/api/v1/roles/assignments", { userId: assignUserId, roleId: assignRoleId });
      const r = await client.get<UserRoleResponse[]>(`/api/v1/roles/assignments/users/${assignUserId}`);
      setAssignments(r.data);
    } catch {
      setError("Could not assign role.");
    }
  }

  async function unassign(assignmentId: string) {
    setError(null);
    try {
      await client.delete(`/api/v1/roles/assignments/${assignmentId}`);
      setAssignments((prev) => prev.filter((a) => a.id !== assignmentId));
    } catch {
      setError("Could not remove role assignment.");
    }
  }

  function roleName(id: string): string {
    return roles.find((r) => r.id === id)?.name ?? id;
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage roles.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Roles &amp; Permissions</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}

      <div className="bg-white shadow rounded p-4 mb-6 max-w-2xl">
        <h3 className="font-semibold mb-2">Create custom role</h3>
        <div className="flex gap-2 mb-2">
          <input className="border rounded px-2 py-1" placeholder="code (e.g. CLAIMS_REVIEWER)" value={code} onChange={(e) => setCode(e.target.value)} />
          <input className="border rounded px-2 py-1" placeholder="Display name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="flex flex-wrap gap-2 mb-2 max-h-32 overflow-y-auto">
          {permissions.map((p) => (
            <label key={p.id} className="text-xs flex items-center gap-1 border rounded px-2 py-1">
              <input type="checkbox" checked={selectedPermissions.has(p.id)} onChange={() => togglePermission(p.id)} />
              {p.resource}:{p.action}
            </label>
          ))}
        </div>
        <button className="btn-primary" onClick={createRole} disabled={!code || !name}>
          Create role
        </button>
      </div>

      <table className="w-full bg-white shadow rounded mb-6">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Code</th>
            <th className="p-2">Name</th>
            <th className="p-2">System</th>
            <th className="p-2">Permissions</th>
          </tr>
        </thead>
        <tbody>
          {roles.map((role) => (
            <tr key={role.id} className="border-b text-sm align-top">
              <td className="p-2 font-mono">{role.code}</td>
              <td className="p-2">{role.name}</td>
              <td className="p-2">{role.system ? "yes" : "no"}</td>
              <td className="p-2 text-xs text-gray-500">
                {role.permissions.map((p) => `${p.resource}:${p.action}`).join(", ") || "—"}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      <div className="bg-white shadow rounded p-4 max-w-2xl">
        <h3 className="font-semibold mb-2">Assign role to user</h3>
        <div className="flex gap-2 mb-2">
          <select className="border rounded px-2 py-1" value={assignUserId} onChange={(e) => setAssignUserId(e.target.value)}>
            <option value="">Select user…</option>
            {users.map((u) => (
              <option key={u.id} value={u.id}>
                {u.displayName} ({u.email})
              </option>
            ))}
          </select>
          <select className="border rounded px-2 py-1" value={assignRoleId} onChange={(e) => setAssignRoleId(e.target.value)}>
            <option value="">Select role…</option>
            {roles.map((r) => (
              <option key={r.id} value={r.id}>
                {r.name}
              </option>
            ))}
          </select>
          <button className="btn-primary" onClick={assignRole} disabled={!assignUserId || !assignRoleId}>
            Assign
          </button>
        </div>
        {assignUserId && (
          <ul className="text-sm">
            {assignments.map((a) => (
              <li key={a.id} className="flex justify-between border-b py-1">
                <span>{roleName(a.roleId)}</span>
                <button className="text-red-600" onClick={() => unassign(a.id)}>
                  Remove
                </button>
              </li>
            ))}
            {assignments.length === 0 && <li className="text-gray-500">No roles assigned.</li>}
          </ul>
        )}
      </div>
    </div>
  );
}
