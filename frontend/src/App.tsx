import { Navigate, Route, Routes } from "react-router-dom";
import { useAuth } from "react-oidc-context";
import { Layout } from "./components/Layout";
import { LoginPage } from "./pages/LoginPage";
import { CallbackPage } from "./pages/CallbackPage";
import { DashboardPage } from "./pages/DashboardPage";
import { DidsPage } from "./pages/DidsPage";
import { CredentialsPage } from "./pages/CredentialsPage";
import { VerificationsPage } from "./pages/VerificationsPage";
import { NotificationsPage } from "./pages/NotificationsPage";
import { OrganizationsPage } from "./pages/OrganizationsPage";
import { TenantsPage } from "./pages/TenantsPage";
import { UsersPage } from "./pages/UsersPage";
import { InvitationsPage } from "./pages/InvitationsPage";
import { RolesPage } from "./pages/RolesPage";
import { ApiClientsPage } from "./pages/ApiClientsPage";
import { WebhooksPage } from "./pages/WebhooksPage";
import { ApiLogsPage } from "./pages/ApiLogsPage";
import { SchemasPage } from "./pages/SchemasPage";
import { TemplatesPage } from "./pages/TemplatesPage";
import { PoliciesPage } from "./pages/PoliciesPage";
import { TrustRegistryPage } from "./pages/TrustRegistryPage";
import { AuditLogPage } from "./pages/AuditLogPage";
import { WalletApp } from "./wallet/WalletApp";

// Set by the `build:wallet` script (via .env.wallet) so the Capacitor-wrapped mobile app ships
// only the holder-facing wallet, never the admin console routes below - kept as a Vite build-time
// flag rather than a runtime route guard since the wallet app has no legitimate reason to even
// bundle the admin pages' code.
const isWalletTarget = import.meta.env.VITE_APP_TARGET === "wallet";

/**
 * E15 UI scaffold plus the post-E15 admin-console build-out: every route here calls a genuinely
 * existing backend endpoint (no mocked data). Still an explicitly-scoped subset of the Baseline's
 * full 36-48 screen inventory (e.g. no Hedera anchoring detail view, no K8s ops console) - see the
 * root README's E15 section for what remains deferred.
 */
export function App() {
  const auth = useAuth();

  if (auth.isLoading) {
    return <p className="p-6 text-gray-600">Loading…</p>;
  }

  if (!auth.isAuthenticated) {
    return (
      <Routes>
        <Route path="/callback" element={<CallbackPage />} />
        <Route path="*" element={<LoginPage />} />
      </Routes>
    );
  }

  if (isWalletTarget) {
    return <WalletApp />;
  }

  return (
    <Routes>
      <Route path="/wallet/*" element={<WalletApp />} />
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="dids" element={<DidsPage />} />
        <Route path="credentials" element={<CredentialsPage />} />
        <Route path="verifications" element={<VerificationsPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="organizations" element={<OrganizationsPage />} />
        <Route path="tenants" element={<TenantsPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="invitations" element={<InvitationsPage />} />
        <Route path="roles" element={<RolesPage />} />
        <Route path="api-clients" element={<ApiClientsPage />} />
        <Route path="webhooks" element={<WebhooksPage />} />
        <Route path="api-logs" element={<ApiLogsPage />} />
        <Route path="schemas" element={<SchemasPage />} />
        <Route path="templates" element={<TemplatesPage />} />
        <Route path="policies" element={<PoliciesPage />} />
        <Route path="trust-registry" element={<TrustRegistryPage />} />
        <Route path="audit-log" element={<AuditLogPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
