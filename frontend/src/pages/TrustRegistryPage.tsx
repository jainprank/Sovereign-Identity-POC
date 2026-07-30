import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

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

  const [selectedRegistry, setSelectedRegistry] = useState<string | null>(null);
  const [issuers, setIssuers] = useState<IssuerResponse[]>([]);
  const [issuerDid, setIssuerDid] = useState("");
  const [organization, setOrganization] = useState("");
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
    try {
      await client.post("/api/v1/trust-registries", { name, framework: framework || null });
      setName("");
      setFramework("");
      loadRegistries();
    } catch {
      setError("Could not create trust registry - requires PLATFORM_ADMIN or ORG_ADMIN.");
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
    try {
      await client.post(`/api/v1/trust-registries/${selectedRegistry}/issuers`, { issuerDid, organization: organization || null });
      setIssuerDid("");
      setOrganization("");
      selectRegistry(selectedRegistry);
    } catch {
      setError("Could not register issuer.");
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
        <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={createRegistry} disabled={!name}>
          Create registry
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
                <button className="text-blue-600" onClick={() => selectRegistry(registry.id)}>
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
            <button className="bg-blue-600 text-white rounded px-4 py-1" onClick={registerIssuer} disabled={!issuerDid}>
              Register issuer
            </button>
          </div>
          <table className="w-full bg-white shadow rounded mb-6">
            <thead>
              <tr className="text-left text-sm text-gray-500 border-b">
                <th className="p-2">Issuer DID</th>
                <th className="p-2">Organization</th>
                <th className="p-2">Status</th>
                <th className="p-2"></th>
              </tr>
            </thead>
            <tbody>
              {issuers.map((issuer) => (
                <tr key={issuer.id} className="border-b text-sm">
                  <td className="p-2 font-mono text-xs">{issuer.issuerDid}</td>
                  <td className="p-2">{issuer.organization ?? "—"}</td>
                  <td className="p-2">{issuer.status}</td>
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
                    <button className="text-blue-600" onClick={() => viewEvents(issuer.id)}>
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
    </div>
  );
}
