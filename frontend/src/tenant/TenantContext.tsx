import { createContext, useContext, useState, type ReactNode } from "react";

const STORAGE_KEY = "idtrust.tenantId";

interface TenantContextValue {
  tenantId: string | null;
  setTenantId: (tenantId: string) => void;
  clearTenantId: () => void;
}

const TenantContext = createContext<TenantContextValue | undefined>(undefined);

/**
 * Holds the active tenant UUID the user is operating as, sent as X-Tenant-ID on every API call
 * (see src/lib/api.ts). There is no "list my tenants" backend endpoint yet (a documented gap -
 * see the root README's E15 section), so this is entered once by the user and persisted to
 * localStorage rather than resolved automatically from the JWT.
 */
export function TenantProvider({ children }: { children: ReactNode }) {
  const [tenantId, setTenantIdState] = useState<string | null>(() => localStorage.getItem(STORAGE_KEY));

  function setTenantId(next: string) {
    localStorage.setItem(STORAGE_KEY, next);
    setTenantIdState(next);
  }

  function clearTenantId() {
    localStorage.removeItem(STORAGE_KEY);
    setTenantIdState(null);
  }

  return (
    <TenantContext.Provider value={{ tenantId, setTenantId, clearTenantId }}>{children}</TenantContext.Provider>
  );
}

export function useTenant(): TenantContextValue {
  const value = useContext(TenantContext);
  if (!value) {
    throw new Error("useTenant must be used within a TenantProvider");
  }
  return value;
}
