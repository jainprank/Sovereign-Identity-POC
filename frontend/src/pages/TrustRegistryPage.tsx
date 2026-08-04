import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import { HederaAnchorLink } from "../components/HederaAnchorLink";

interface RegistryResponse {
  id: string;
  name: string;
  framework: string | null;
  status: "ACTIVE" | "INACTIVE";
  createdAt: string;
}

interface IssuerResponse {
  id: string;
  issuerDid: string;
  organization: string | null;
  trustedCredentialTypes: string[] | null;
  status: "TRUSTED" | "SUSPENDED" | "REVOKED";
  registeredAt: string;
}

interface EventResponse {
  id: string;
  previousStatus: string;
  newStatus: string;
  reason: string | null;
  occurredAt: string;
}

/** Dynamic issuer trust management (FR-TRU, E12) - registries, their trusted issuers, and issuer status history. */
export function TrustRegistryPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [registries, setRegistries] = useState<RegistryResponse[]>([]);
  const [name, setName] = useState("");
  const [framework, setFramework] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [creatingRegistry, setCreatingRegistry] = useState(false);
  const [createdRegistry, setCreatedRegistry] = useState<RegistryResponse | null>(null);

  const [selectedRegistry, setSelectedRegistry] = useState<string | null>(null);
  const [issuers, setIssuers] = useState<IssuerResponse[]>([]);
  const [issuerDid, setIssuerDid] = useState("");
  const [organization, setOrganization] = useState("");
  const [registeringIssuer, setRegisteringIssuer] = useState(false);
  const [registeredIssuer, setRegisteredIssuer] = useState<IssuerResponse | null>(null);
  const [events, setEvents] = useState<EventResponse[]>([]);
  const [eventsFor, setEventsFor] = useState<string | null>(null);

  function loadRegistries() {
    if (!tenantId) return;
    client
      .get<RegistryResponse[]>("/api/v1/trust-registries")
      .then((r) => setRegistries(r.data))
      .catch(() => setError("Could not load trust registries."));
  }

  useEffect(loadRegistries, [tenantId]);

  async function createRegistry() {
    setError(null);
    setCreatingRegistry(true);
    try {
      const response = await client.post<RegistryResponse>("/api/v1/trust-registries", { name, framework: framework || null });
      setName("");
      setFramework("");
      loadRegistries();
      setCreatedRegistry(response.data);
    } catch {
      setError("Could not create trust registry - requires PLATFORM_ADMIN or ORG_ADMIN.");
    } finally {
      setCreatingRegistry(false);
    }
  }

  async function setRegistryStatus(id: string, action: "activate" | "deactivate") {
    setError(null);
    try {
      await client.post(`/api/v1/trust-registries/${id}/${action}`);
      loadRegistries();
    } catch {
      setError(`Could not ${action} registry.`);
    }
  }

  function selectRegistry(id: string) {
    setSelectedRegistry(id);
    setEventsFor(null);
    client
      .get<IssuerResponse[]>(`/api/v1/trust-registries/${id}/issuers`)
      .then((r) => setIssuers(r.data))
      .catch(() => setIssuers([]));
  }

  async function registerIssuer() {
    if (!selectedRegistry) return;
    setError(null);
    setRegisteringIssuer(true);
    try {
      const response = await client.post<IssuerResponse>(`/api/v1/trust-registries/${selectedRegistry}/issuers`, {
        issuerDid,
        organization: organization || null,
      });
      setIssuerDid("");
      setOrganization("");
      selectRegistry(selectedRegistry);
      setRegisteredIssuer(response.data);
    } catch {
      setError("Could not register issuer.");
    } finally {
      setRegisteringIssuer(false);
    }
  }

  async function updateIssuerStatus(issuerId: string, status: string) {
    if (!selectedRegistry) return;
    setError(null);
    try {
      await client.patch(`/api/v1/trust-registries/${selectedRegistry}/issuers/${issuerId}/status`, { status });
      selectRegistry(selectedRegistry);
    } catch {
      setError("Could not update issuer status.");
    }
  }

  async function revokeIssuer(issuerId: string) {
    if (!selectedRegistry) return;
    setError(null);
    try {
      await client.post(`/api/v1/trust-registries/${selectedRegistry}/issuers/${issuerId}/revoke`);
      selectRegistry(selectedRegistry);
    } catch {
      setError("Could not revoke issuer.");
    }
  }

  function viewEvents(issuerId: string) {
    if (!selectedRegistry) return;
    setEventsFor(issuerId);
    client
      .get<EventResponse[]>(`/api/v1/trust-registries/${selectedRegistry}/issuers/${issuerId}/events`)
      .then((r) => setEvents(r.data))
      .catch(() => setEvents([]));
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage the trust registry.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Trust Registry</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}

      <div className="flex gap-2 mb-4">
        <input className="border rounded px-2 py-1" placeholder="Registry name" value={name} onChange={(e) => setName(e.target.value)} />
        <input className="border rounded px-2 py-1" placeholder="Framework (optional)" value={framework} onChange={(e) => setFramework(e.target.value)} />
        <button className="btn-primary" onClick={createRegistry} disabled={!name || creatingRegistry}>
          {creatingRegistry ? (
            <span className="inline-flex items-center gap-2">
              <span
                className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"
                aria-hidden="true"
              />
              Creating…
            </span>
          ) : (
            "Create registry"
          )}
        </button>
      </div>

      <table className="w-full bg-white shadow rounded mb-6">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Name</th>
            <th className="p-2">Framework</th>
            <th className="p-2">Status</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {registries.map((registry) => (
            <tr key={registry.id} className="border-b text-sm">
              <td className="p-2">{registry.name}</td>
              <td className="p-2">{registry.framework ?? "—"}</td>
              <td className="p-2">{registry.status}</td>
              <td className="p-2 flex gap-2">
                <button className="link-primary" onClick={() => selectRegistry(registry.id)}>
                  Issuers
                </button>
                {registry.status === "ACTIVE" ? (
                  <button className="text-yellow-600" onClick={() => setRegistryStatus(registry.id, "deactivate")}>
                    Deactivate
                  </button>
                ) : (
                  <button className="text-green-600" onClick={() => setRegistryStatus(registry.id, "activate")}>
                    Activate
                  </button>
                )}
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {selectedRegistry && (
        <div>
          <h3 className="font-semibold mb-2">Trusted issuers</h3>
          <div className="flex gap-2 mb-4">
            <input className="border rounded px-2 py-1 w-96" placeholder="Issuer DID" value={issuerDid} onChange={(e) => setIssuerDid(e.target.value)} />
            <input className="border rounded px-2 py-1" placeholder="Organization (optional)" value={organization} onChange={(e) => setOrganization(e.target.value)} />
            <button className="btn-primary" onClick={registerIssuer} disabled={!issuerDid || registeringIssuer}>
              {registeringIssuer ? (
                <span className="inline-flex items-center gap-2">
                  <span
                    className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"
                    aria-hidden="true"
                  />
                  Registering…
                </span>
              ) : (
                "Register issuer"
              )}
            </button>
          </div>
          <table className="w-full bg-white shadow rounded mb-6">
            <thead>
              <tr className="text-left text-sm text-gray-500 border-b">
                <th className="p-2">Issuer DID</th>
                <th className="p-2">Organization</th>
                <th className="p-2">Status</th>
                <th className="p-2">Hedera anchor</th>
                <th className="p-2"></th>
              </tr>
            </thead>
            <tbody>
              {issuers.map((issuer) => (
                <tr key={issuer.id} className="border-b text-sm">
                  <td className="p-2 font-mono text-xs">{issuer.issuerDid}</td>
                  <td className="p-2">{issuer.organization ?? "—"}</td>
                  <td className="p-2">{issuer.status}</td>
                  <td className="p-2">
                    <HederaAnchorLink api={client} resourceType="TRUSTED_ISSUER" resourceId={issuer.id} />
                  </td>
                  <td className="p-2 flex gap-2">
                    {issuer.status !== "TRUSTED" && (
                      <button className="text-green-600" onClick={() => updateIssuerStatus(issuer.id, "TRUSTED")}>
                        Trust
                      </button>
                    )}
                    {issuer.status === "TRUSTED" && (
                      <button className="text-yellow-600" onClick={() => updateIssuerStatus(issuer.id, "SUSPENDED")}>
                        Suspend
                      </button>
                    )}
                    {issuer.status !== "REVOKED" && (
                      <button className="text-red-600" onClick={() => revokeIssuer(issuer.id)}>
                        Revoke
                      </button>
                    )}
                    <button className="link-primary" onClick={() => viewEvents(issuer.id)}>
                      History
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>

          {eventsFor && (
            <ul className="text-sm bg-white shadow rounded p-3">
              {events.map((event) => (
                <li key={event.id} className="border-b py-1">
                  {event.previousStatus} → {event.newStatus} at {new Date(event.occurredAt).toLocaleString()}
                  {event.reason ? ` (${event.reason})` : ""}
                </li>
              ))}
              {events.length === 0 && <li className="text-gray-500">No status history.</li>}
            </ul>
          )}
        </div>
      )}

      {createdRegistry && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setCreatedRegistry(null)}>
          <div className="bg-white rounded shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-green-100 text-green-600 text-lg">✓</span>
              <h3 className="text-lg font-semibold">Trust registry created</h3>
            </div>
            <p className="text-sm text-gray-600 mb-4">
              <span className="font-medium">{createdRegistry.name}</span> is active and ready to register trusted issuers.
            </p>
            <div className="flex justify-end">
              <button className="btn-primary" onClick={() => setCreatedRegistry(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {registeredIssuer && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setRegisteredIssuer(null)}>
          <div className="bg-white rounded shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-green-100 text-green-600 text-lg">✓</span>
              <h3 className="text-lg font-semibold">Issuer registered</h3>
            </div>
            <p className="text-sm text-gray-600 mb-3">
              The issuer has been registered as trusted and anchored to Hedera.
            </p>
            <div className="bg-gray-50 rounded p-2 mb-4 font-mono text-xs break-all">{registeredIssuer.issuerDid}</div>
            <div className="flex items-center justify-between">
              <HederaAnchorLink api={client} resourceType="TRUSTED_ISSUER" resourceId={registeredIssuer.id} />
              <button className="btn-primary" onClick={() => setRegisteredIssuer(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
