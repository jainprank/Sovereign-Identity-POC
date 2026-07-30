import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useTenant } from "../tenant/TenantContext";

interface NavItem {
  to: string;
  label: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

const navGroups: NavGroup[] = [
  { label: "Overview", items: [{ to: "/", label: "Dashboard" }] },
  {
    label: "Identity",
    items: [
      { to: "/organizations", label: "Organizations" },
      { to: "/tenants", label: "Tenants" },
      { to: "/users", label: "Users" },
      { to: "/invitations", label: "Invitations" },
      { to: "/roles", label: "Roles & Permissions" },
    ],
  },
  {
    label: "Credentials",
    items: [
      { to: "/dids", label: "DIDs" },
      { to: "/schemas", label: "Schemas" },
      { to: "/templates", label: "Templates" },
      { to: "/credentials", label: "Issue & Browse" },
    ],
  },
  {
    label: "Verification",
    items: [
      { to: "/verifications", label: "Requests" },
      { to: "/policies", label: "Policies" },
      { to: "/trust-registry", label: "Trust Registry" },
    ],
  },
  {
    label: "Developer Platform",
    items: [
      { to: "/api-clients", label: "API Clients" },
      { to: "/webhooks", label: "Webhooks" },
      { to: "/api-logs", label: "API Request Logs" },
    ],
  },
  {
    label: "Operations",
    items: [
      { to: "/audit-log", label: "Audit Log" },
      { to: "/notifications", label: "Notifications" },
    ],
  },
];

export function Layout() {
  const auth = useAuth();
  const { tenantId, setTenantId } = useTenant();

  return (
    <div className="min-h-screen bg-gray-50 flex">
      <aside className="w-56 bg-white border-r border-gray-200 flex flex-col shrink-0">
        <div className="px-4 py-3 border-b border-gray-200">
          <span className="font-semibold text-lg">IDTrust</span>
        </div>
        <nav className="flex-1 overflow-y-auto py-2">
          {navGroups.map((group) => (
            <div key={group.label} className="mb-3">
              <p className="px-4 pt-2 pb-1 text-xs font-semibold uppercase text-gray-400">{group.label}</p>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) =>
                    "block px-4 py-1.5 text-sm " + (isActive ? "font-semibold text-blue-600 bg-blue-50" : "text-gray-600 hover:bg-gray-50")
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>
      </aside>
      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-end gap-4">
          <input
            className="border rounded px-2 py-1 text-sm w-72"
            placeholder="Tenant ID (UUID)"
            defaultValue={tenantId ?? ""}
            onBlur={(event) => event.target.value.trim() && setTenantId(event.target.value.trim())}
          />
          <span className="text-sm text-gray-500">{auth.user?.profile.email}</span>
          <button className="text-sm text-red-600" onClick={() => auth.removeUser()}>
            Log out
          </button>
        </header>
        <main className="p-6 overflow-y-auto">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
