import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// Minimal Vite config. VITE_API_BASE_URL/VITE_KEYCLOAK_* env vars (see .env.example) are read at
// runtime via import.meta.env - no dev-server proxy needed since the backend exposes CORS for
// this dev server's origin directly (idtrust.security.cors.allowed-origins, default
// http://localhost:5173 - see identity.security.SecurityConfig/SecurityCorsProperties).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
  },
});
