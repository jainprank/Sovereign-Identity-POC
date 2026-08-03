import { Capacitor } from "@capacitor/core";
import type { AuthProviderProps } from "react-oidc-context";
import type { User } from "oidc-client-ts";
import { UserManager } from "oidc-client-ts";

// Authorization Code + PKCE against Keycloak (Master Spec section 11 / Baseline section 24 -
// this frontend is a public OIDC client, never holding a client secret in the browser). Mirrors
// KEYCLOAK_URL/KEYCLOAK_REALM/KEYCLOAK_CLIENT_ID already declared in the root .env.example for
// the backend's own resource-server config - same realm/client, different (public) grant type.
//
// On the Capacitor-wrapped native app, the redirect can't stay inside the WebView the way it does
// on web: Capacitor hands top-level navigation to a different origin off to the system browser
// (RFC 8252's recommended pattern anyway - a proper external user-agent instead of an in-app
// WebView). That browser can't navigate back into the app's `http://localhost` origin, so the
// redirect_uri is instead a custom URL scheme (registered as an intent-filter on MainActivity in
// AndroidManifest.xml and as a valid redirect URI on the Keycloak client) that the OS routes back
// into the app; see nativeDeepLink.ts for the other half (feeding that URL back into oidc-client-ts).
const isNative = Capacitor.isNativePlatform();
const NATIVE_REDIRECT_URI = "com.idtrust.wallet://callback";

export const oidcSettings = {
  authority: `${import.meta.env.VITE_KEYCLOAK_URL}/realms/${import.meta.env.VITE_KEYCLOAK_REALM}`,
  client_id: import.meta.env.VITE_KEYCLOAK_CLIENT_ID,
  redirect_uri: isNative ? NATIVE_REDIRECT_URI : `${window.location.origin}/callback`,
  post_logout_redirect_uri: isNative ? NATIVE_REDIRECT_URI : window.location.origin,
  scope: "openid profile email",
};

export const userManager = new UserManager(oidcSettings);

export const onSigninCallback: AuthProviderProps["onSigninCallback"] = (user: User | void) => {
  if (isNative) {
    // Nothing to clean up in the WebView's own URL - the redirect happened in the system
    // browser, not here.
    return;
  }
  const returnTo = (user?.state as { returnTo?: string } | undefined)?.returnTo || "/";
  window.history.replaceState({}, document.title, returnTo);
};
