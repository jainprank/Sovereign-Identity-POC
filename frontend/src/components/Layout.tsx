import { ReactNode } from "react";
import { NavLink, Outlet, useLocation } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { useTenant } from "../tenant/TenantContext";

interface NavItem {
  to: string;
  label: string;
  icon: ReactNode;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

function Icon({ children }: { children: ReactNode }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2} className="h-4 w-4 shrink-0 opacity-60 group-hover:opacity-80">
      {children}
    </svg>
  );
}

const navGroups: NavGroup[] = [
  {
    label: "Overview",
    items: [
      {
        to: "/",
        label: "Dashboard",
        icon: (
          <Icon>
            <rect x="3" y="3" width="7" height="7" rx="1" />
            <rect x="14" y="3" width="7" height="7" rx="1" />
            <rect x="3" y="14" width="7" height="7" rx="1" />
            <rect x="14" y="14" width="7" height="7" rx="1" />
          </Icon>
        ),
      },
    ],
  },
  {
    label: "Identity",
    items: [
      {
        to: "/organizations",
        label: "Organizations",
        icon: (
          <Icon>
            <path d="M3 21h18M6 21V7l6-4 6 4v14M9 9h1m4 0h1m-5 4h1m4 0h1m-5 4h1m4 0h1" />
          </Icon>
        ),
      },
      {
        to: "/tenants",
        label: "Tenants",
        icon: (
          <Icon>
            <rect x="3" y="4" width="18" height="16" rx="2" />
            <path d="M3 10h18M8 4v4M16 4v4" />
          </Icon>
        ),
      },
      {
        to: "/users",
        label: "Users",
        icon: (
          <Icon>
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" />
            <circle cx="9" cy="7" r="4" />
            <path d="M23 21v-2a4 4 0 00-3-3.87M16 3.13a4 4 0 010 7.75" />
          </Icon>
        ),
      },
      {
        to: "/invitations",
        label: "Invitations",
        icon: (
          <Icon>
            <rect x="2" y="4" width="20" height="16" rx="2" />
            <path d="M22 6l-10 7L2 6" />
          </Icon>
        ),
      },
      {
        to: "/roles",
        label: "Roles & Permissions",
        icon: (
          <Icon>
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
          </Icon>
        ),
      },
    ],
  },
  {
    label: "Credentials",
    items: [
      {
        to: "/dids",
        label: "DIDs",
        icon: (
          <Icon>
            <path d="M21 8a9 9 0 11-9-9m0 4a5 5 0 100 10 5 5 0 000-10z" />
          </Icon>
        ),
      },
      {
        to: "/schemas",
        label: "Schemas",
        icon: (
          <Icon>
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
            <polyline points="14 2 14 8 20 8" />
          </Icon>
        ),
      },
      {
        to: "/templates",
        label: "Templates",
        icon: (
          <Icon>
            <rect x="3" y="3" width="18" height="18" rx="2" />
            <path d="M3 9h18M9 21V9" />
          </Icon>
        ),
      },
      {
        to: "/credentials",
        label: "Issue & Browse",
        icon: (
          <Icon>
            <rect x="2" y="7" width="20" height="14" rx="2" />
            <path d="M16 7V5a2 2 0 00-2-2h-4a2 2 0 00-2 2v2" />
            <circle cx="12" cy="13" r="2" />
          </Icon>
        ),
      },
    ],
  },
  {
    label: "Verification",
    items: [
      {
        to: "/verifications",
        label: "Requests",
        icon: (
          <Icon>
            <circle cx="11" cy="11" r="7" />
            <path d="M21 21l-4.35-4.35" />
          </Icon>
        ),
      },
      {
        to: "/policies",
        label: "Policies",
        icon: (
          <Icon>
            <rect x="3" y="11" width="18" height="11" rx="2" />
            <path d="M7 11V7a5 5 0 0110 0v4" />
          </Icon>
        ),
      },
      {
        to: "/trust-registry",
        label: "Trust Registry",
        icon: (
          <Icon>
            <path d="M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
          </Icon>
        ),
      },
    ],
  },
  {
    label: "Developer Platform",
    items: [
      {
        to: "/api-clients",
        label: "API Clients",
        icon: (
          <Icon>
            <polyline points="16 18 22 12 16 6" />
            <polyline points="8 6 2 12 8 18" />
          </Icon>
        ),
      },
      {
        to: "/webhooks",
        label: "Webhooks",
        icon: (
          <Icon>
            <circle cx="6" cy="18" r="3" />
            <circle cx="18" cy="6" r="3" />
            <path d="M9 18h6a3 3 0 003-3v-2M15 6H9a3 3 0 00-3 3v2" />
          </Icon>
        ),
      },
      {
        to: "/api-logs",
        label: "API Request Logs",
        icon: (
          <Icon>
            <path d="M3 3h18v18H3z" />
            <path d="M7 8h10M7 12h10M7 16h6" />
          </Icon>
        ),
      },
    ],
  },
  {
    label: "Operations",
    items: [
      {
        to: "/audit-log",
        label: "Audit Log",
        icon: (
          <Icon>
            <path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z" />
            <polyline points="14 2 14 8 20 8" />
            <line x1="16" y1="13" x2="8" y2="13" />
            <line x1="16" y1="17" x2="8" y2="17" />
          </Icon>
        ),
      },
      {
        to: "/notifications",
        label: "Notifications",
        icon: (
          <Icon>
            <path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
            <path d="M13.73 21a2 2 0 01-3.46 0" />
          </Icon>
        ),
      },
    ],
  },
];

function currentPageLabel(pathname: string): string {
  for (const group of navGroups) {
    for (const item of group.items) {
      const isMatch = item.to === "/" ? pathname === "/" : pathname.startsWith(item.to);
      if (isMatch) return item.label;
    }
  }
  return "IDTrust";
}

export function Layout() {
  const auth = useAuth();
  const { tenantId, setTenantId } = useTenant();
  const location = useLocation();
  const email = auth.user?.profile.email ?? "";
  const initials = email ? email.slice(0, 2).toUpperCase() : "ID";

  return (
    <div className="min-h-screen bg-page">
      <aside className="fixed inset-y-0 left-0 z-50 flex w-[260px] flex-col border-r border-surface-200 bg-sidebar">
        <div className="flex items-center gap-2.5 border-b border-surface-200 px-4 py-4">
          <div className="flex h-[34px] w-[34px] shrink-0 items-center justify-center rounded-lg bg-primary">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="white">
              <path d="M12 2l8 4v6c0 5-3.5 8-8 10-4.5-2-8-5-8-10V6z" />
            </svg>
          </div>
          <div className="leading-tight">
            <strong className="block text-[13px] font-bold text-ink">IDTrust</strong>
            <span className="text-[10px] font-semibold uppercase tracking-wide text-primary">Admin Console</span>
          </div>
        </div>

        <nav className="flex-1 overflow-y-auto px-1.5 py-2">
          {navGroups.map((group) => (
            <div key={group.label} className="mb-1">
              <p className="mt-2 px-2.5 pb-0.5 pt-1 text-[10px] font-semibold uppercase tracking-wide text-surface-400">
                {group.label}
              </p>
              {group.items.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.to === "/"}
                  className={({ isActive }) =>
                    "group mb-0.5 flex h-9 items-center gap-2.5 rounded-md px-3 text-[13px] font-medium transition-colors " +
                    (isActive ? "bg-primary-dim text-primary" : "text-ink2 hover:bg-primary-dim hover:text-ink")
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}
        </nav>

        <div className="border-t border-surface-200 p-3">
          <div className="mb-2.5 flex items-center gap-2">
            <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-primary text-xs font-bold text-white">
              {initials}
            </div>
            <div className="min-w-0 flex-1 leading-tight">
              <strong className="block truncate text-xs font-semibold text-ink">
                {auth.user?.profile.name ?? "Signed in"}
              </strong>
              <span className="block truncate text-[10px] text-ink3">{email}</span>
            </div>
            <button
              className="flex shrink-0 items-center justify-center rounded p-1 text-danger hover:bg-danger-bg"
              title="Log out"
              onClick={() => auth.removeUser()}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth={2}>
                <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" />
                <polyline points="16 17 21 12 16 7" />
                <line x1="21" y1="12" x2="9" y2="12" />
              </svg>
            </button>
          </div>
        </div>
      </aside>

      <div className="ml-[260px] flex min-h-screen flex-col">
        <header className="sticky top-0 z-40 flex h-[68px] items-center justify-between border-b border-surface-200 bg-white px-6">
          <div>
            <h1 className="text-base font-semibold text-ink">{currentPageLabel(location.pathname)}</h1>
          </div>
          <div className="flex items-center gap-3">
            <input
              className="w-72 rounded border border-surface-300 px-2.5 py-1.5 text-xs font-mono focus:border-primary focus:outline-none"
              placeholder="Tenant ID (UUID)"
              defaultValue={tenantId ?? ""}
              onBlur={(event) => event.target.value.trim() && setTenantId(event.target.value.trim())}
            />
          </div>
        </header>
        <main className="flex-1 overflow-y-auto p-7">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
