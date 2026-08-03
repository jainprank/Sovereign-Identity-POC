import { Capacitor } from "@capacitor/core";
import { App } from "@capacitor/app";
import { userManager } from "./oidcConfig";

// Completes the native sign-in flow: the system browser finishes the Keycloak login and
// redirects to the com.idtrust.wallet://callback custom scheme (see oidcConfig.ts), which Android
// routes back into this app as an appUrlOpen event rather than a WebView navigation. Feed that URL
// into the same UserManager instance react-oidc-context's AuthProvider is listening on so its
// state updates the same way it would for a same-window web redirect.
export function setupNativeAuthDeepLink(): void {
  if (!Capacitor.isNativePlatform()) {
    return;
  }
  App.addListener("appUrlOpen", ({ url }) => {
    if (!url.startsWith("com.idtrust.wallet://callback")) {
      return;
    }
    userManager.signinRedirectCallback(url).catch((error) => {
      console.error("Native sign-in callback failed", error);
    });
  });
}
