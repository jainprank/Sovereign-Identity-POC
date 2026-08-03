import type { CapacitorConfig } from "@capacitor/core";

// Wraps the wallet-only build (see `build:wallet` in package.json / .env.wallet) as an installable
// native app shell. `webDir` points at the wallet-mode Vite output, not the admin console build.
const config: CapacitorConfig = {
  appId: "com.idtrust.wallet",
  appName: "IDTrust Wallet",
  webDir: "dist-wallet",
  server: {
    // Capacitor serves the app over https://localhost on Android by default. The local dev
    // backend/Keycloak are plain HTTP, so fetches to them from an https origin get silently
    // blocked as mixed content (distinct from, and in addition to, CORS). Forcing http here keeps
    // the app's own origin on the same scheme as the dev backend it talks to.
    //
    // Deliberately no `allowNavigation` entry for Keycloak's origin: Capacitor's WebView bridge
    // intercepts *all* requests (not just top-level navigation) to any host listed there as "local
    // requests" through its own asset-serving layer, which breaks real CORS handling for XHR/fetch
    // to that host (see oidcConfig.ts/nativeDeepLink.ts - the OIDC redirect goes to the system
    // browser instead, which is the RFC 8252-recommended pattern anyway).
    androidScheme: "http",
  },
};

export default config;
