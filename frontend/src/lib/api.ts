import axios, { type AxiosInstance } from "axios";

/**
 * One axios instance per render, carrying the caller's Keycloak bearer token and the
 * X-Tenant-ID header every tenant-scoped backend endpoint requires (three-layer tenant
 * isolation - see backend identity.security.TenantMembershipFilter's Javadoc). Callers get
 * both values from useAuth() (react-oidc-context) and useTenant() (src/tenant/TenantContext.tsx)
 * respectively and pass them in here rather than this module reaching into global state, so
 * every request is explicitly scoped to whoever is rendering at call time.
 */
export function createApiClient(accessToken: string | undefined, tenantId: string | null): AxiosInstance {
  const client = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL,
  });
  client.interceptors.request.use((config) => {
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    if (tenantId) {
      config.headers["X-Tenant-ID"] = tenantId;
    }
    return config;
  });
  return client;
}
