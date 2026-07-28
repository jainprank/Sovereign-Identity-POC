import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useTenant } from "../tenant/TenantContext";

const navItems = [
  { to: "/", label: "Dashboard" },
  { to: "/dids", label: "DIDs" },
  { to: "/credentials", label: "Credentials" },
  { to: "/verifications", label: "Verifications" },
  { to: "/notifications", label: "Notifications" },
];

export function Layout() {
  const auth = useAuth();
  const { tenantId, setTenantId } = useTenant();

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white border-b border-gray-200 px-6 py-3 flex items-center justify-between">
        <div className="flex items-center gap-6">
          <span className="font-semibold text-lg">IDTrust</span>
          <nav className="flex gap-4">
            {navItems.map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) => (isActive ? "font-semibold text-blue-600" : "text-gray-600")}
              >
                {item.label}
              </NavLink>
            ))}
          </nav>
        </div>
        <div className="flex items-center gap-4">
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
        </div>
      </header>
      <main className="p-6">
        <Outlet />
      </main>
    </div>
  );
}
