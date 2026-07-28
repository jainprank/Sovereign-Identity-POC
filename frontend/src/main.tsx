import React from "react";
import ReactDOM from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import { AuthProvider } from "react-oidc-context";
import { App } from "./App";
import { TenantProvider } from "./tenant/TenantContext";
import { oidcConfig } from "./auth/oidcConfig";
import "./styles/index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <AuthProvider {...oidcConfig}>
      <TenantProvider>
        <BrowserRouter>
          <App />
        </BrowserRouter>
      </TenantProvider>
    </AuthProvider>
  </React.StrictMode>,
);
