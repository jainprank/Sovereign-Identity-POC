import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

const EVENT_TYPES = [
  "CredentialIssued", "CredentialRevoked", "CredentialSuspended", "VerificationCompleted",
  "DIDCreated", "DIDUpdated", "TrustRegistryUpdated", "HederaAnchorRequested",
  "HederaAnchorCompleted", "AuditEventCreated",
];

interface WebhookEndpointResponse {
  id: string;
  url: string;
  eventTypes: string[];
  active: boolean;
  createdAt: string;
}

interface WebhookDeliveryResponse {
  id: string;
  eventType: string;
  resourceType: string;
  resourceId: string;
  status: "DELIVERED" | "FAILED";
  attempts: number;
  lastError: string | null;
  createdAt: string;
}

export function WebhooksPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [endpoints, setEndpoints] = useState<WebhookEndpointResponse[]>([]);
  const [url, setUrl] = useState("");
  const [selectedEvents, setSelectedEvents] = useState<Set<string>>(new Set());
  const [newSecret, setNewSecret] = useState<string | null>(null);
  const [deliveries, setDeliveries] = useState<WebhookDeliveryResponse[]>([]);
  const [deliveriesFor, setDeliveriesFor] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<WebhookEndpointResponse[]>("/api/v1/webhooks")
      .then((r) => setEndpoints(r.data))
      .catch(() => setError("Could not load webhooks - requires PLATFORM_ADMIN or ORG_ADMIN."));
  }

  useEffect(load, [tenantId]);

  function toggleEvent(eventType: string) {
    setSelectedEvents((prev) => {
      const next = new Set(prev);
      if (next.has(eventType)) next.delete(eventType);
      else next.add(eventType);
      return next;
    });
  }

  async function create() {
    setError(null);
    setNewSecret(null);
    try {
      const response = await client.post("/api/v1/webhooks", { url, eventTypes: Array.from(selectedEvents) });
      setNewSecret(response.data.secret);
      setUrl("");
      setSelectedEvents(new Set());
      load();
    } catch {
      setError("Could not create webhook - check the URL and that at least one event type is selected.");
    }
  }

  async function setActive(id: string, active: boolean) {
    setError(null);
    try {
      await client.post(`/api/v1/webhooks/${id}/${active ? "activate" : "deactivate"}`);
      load();
    } catch {
      setError("Could not update webhook.");
    }
  }

  async function viewDeliveries(id: string) {
    setDeliveriesFor(id);
    try {
      const r = await client.get<WebhookDeliveryResponse[]>(`/api/v1/webhooks/${id}/deliveries`);
      setDeliveries(r.data);
    } catch {
      setDeliveries([]);
    }
  }

  async function retry(deliveryId: string) {
    setError(null);
    try {
      await client.post(`/api/v1/webhooks/deliveries/${deliveryId}/retry`);
      if (deliveriesFor) viewDeliveries(deliveriesFor);
    } catch {
      setError("Retry failed.");
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage webhooks.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Webhooks</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      {newSecret && (
        <div className="bg-yellow-50 border border-yellow-300 rounded p-3 mb-4 text-sm">
          <p className="font-semibold">Signing secret shown once - copy it now:</p>
          <p className="font-mono break-all">{newSecret}</p>
        </div>
      )}
      <div className="bg-white shadow rounded p-4 mb-6 max-w-2xl">
        <input
          className="border rounded px-2 py-1 w-full mb-2"
          placeholder="https://example.com/webhook"
          value={url}
          onChange={(e) => setUrl(e.target.value)}
        />
        <div className="flex flex-wrap gap-2 mb-2">
          {EVENT_TYPES.map((eventType) => (
            <label key={eventType} className="text-xs flex items-center gap-1 border rounded px-2 py-1">
              <input type="checkbox" checked={selectedEvents.has(eventType)} onChange={() => toggleEvent(eventType)} />
              {eventType}
            </label>
          ))}
        </div>
        <button className="btn-primary" onClick={create} disabled={!url || selectedEvents.size === 0}>
          Create webhook
        </button>
      </div>

      <table className="w-full bg-white shadow rounded mb-6">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">URL</th>
            <th className="p-2">Events</th>
            <th className="p-2">Active</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {endpoints.map((endpoint) => (
            <tr key={endpoint.id} className="border-b text-sm">
              <td className="p-2 font-mono text-xs">{endpoint.url}</td>
              <td className="p-2 text-xs">{endpoint.eventTypes.join(", ")}</td>
              <td className="p-2">{endpoint.active ? "yes" : "no"}</td>
              <td className="p-2 flex gap-2">
                <button className={endpoint.active ? "text-yellow-600" : "text-green-600"} onClick={() => setActive(endpoint.id, !endpoint.active)}>
                  {endpoint.active ? "Deactivate" : "Activate"}
                </button>
                <button className="link-primary" onClick={() => viewDeliveries(endpoint.id)}>
                  Deliveries
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {deliveriesFor && (
        <div>
          <h3 className="font-semibold mb-2">Deliveries</h3>
          <table className="w-full bg-white shadow rounded">
            <thead>
              <tr className="text-left text-sm text-gray-500 border-b">
                <th className="p-2">Event</th>
                <th className="p-2">Resource</th>
                <th className="p-2">Status</th>
                <th className="p-2">Attempts</th>
                <th className="p-2">Last error</th>
                <th className="p-2"></th>
              </tr>
            </thead>
            <tbody>
              {deliveries.map((delivery) => (
                <tr key={delivery.id} className="border-b text-sm">
                  <td className="p-2">{delivery.eventType}</td>
                  <td className="p-2 text-xs">{delivery.resourceType}/{delivery.resourceId}</td>
                  <td className="p-2">{delivery.status}</td>
                  <td className="p-2">{delivery.attempts}</td>
                  <td className="p-2 text-xs text-red-600">{delivery.lastError ?? "—"}</td>
                  <td className="p-2">
                    {delivery.status === "FAILED" && (
                      <button className="link-primary" onClick={() => retry(delivery.id)}>
                        Retry
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
