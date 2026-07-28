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

/**
 * E15 UI scaffold - an explicitly-scoped subset of the Baseline's full 36-48 screen inventory
 * (Login, Dashboard, DIDs, Credential issuance, Verification requests, Notifications); every
 * other screen (org/user/role administration, schema/policy authoring, webhook/API-client
 * management, trust registry, Hedera anchoring, audit log viewer, etc.) is deferred - see the
 * root README's E15 section for the full "out of scope for this pass" list. Each page here calls
 * a genuinely existing backend endpoint (no mocked data).
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

  return (
    <Routes>
      <Route path="/" element={<Layout />}>
        <Route index element={<DashboardPage />} />
        <Route path="dids" element={<DidsPage />} />
        <Route path="credentials" element={<CredentialsPage />} />
        <Route path="verifications" element={<VerificationsPage />} />
        <Route path="notifications" element={<NotificationsPage />} />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
