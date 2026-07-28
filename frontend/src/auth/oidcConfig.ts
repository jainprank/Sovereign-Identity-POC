import type { AuthProviderProps } from "react-oidc-context";

// Authorization Code + PKCE against Keycloak (Master Spec section 11 / Baseline section 24 -
// this frontend is a public OIDC client, never holding a client secret in the browser). Mirrors
// KEYCLOAK_URL/KEYCLOAK_REALM/KEYCLOAK_CLIENT_ID already declared in the root .env.example for
// the backend's own resource-server config - same realm/client, different (public) grant type.
export const oidcConfig: AuthProviderProps = {
  authority: `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`,
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  redirect_uri: `${window.location.origin}/callback`,
  post_logout_redirect_uri: window.location.origin,
  scope: "openid profile email",
  onSigninCallback: () => {
    window.history.replaceState({}, document.title, window.location.pathname.replace("/callback", "/"));
  },
};
