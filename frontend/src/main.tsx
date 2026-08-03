import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "react-oidc-context";
import { App } from "./App";
import { TenantProvider } from "./tenant/TenantContext";
import { userManager, onSigninCallback } from "./auth/oidcConfig";
import { setupNativeAuthDeepLink } from "./auth/nativeDeepLink";
import "./styles/index.css";

setupNativeAuthDeepLink();

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AuthProvider userManager={userManager} onSigninCallback={onSigninCallback}>
      <TenantProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </TenantProvider>
    </AuthProvider>
  </React.StrictMode>,
);
