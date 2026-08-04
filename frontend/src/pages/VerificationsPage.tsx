import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";

interface PolicyResponse {
  id: string;
  name: string;
}

interface VerificationResponse {
  id: string;
  status: string;
  statusReason: string | null;
  createdAt: string;
  expiresAt: string;
}

export function VerificationsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [policies, setPolicies] = useState<PolicyResponse[]>([]);
  const [requests, setRequests] = useState<VerificationResponse[]>([]);
  const [policyId, setPolicyId] = useState("");
  const [qrCode, setQrCode] = useState<string | null>(null);
  const [requestUri, setRequestUri] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [justCreated, setJustCreated] = useState(false);

  function load() {
    if (!tenantId) return;
    client.get<PolicyResponse[]>("/api/v1/verification-policies").then((r) => setPolicies(r.data)).catch(() => {});
    client.get<VerificationResponse[]>("/api/v1/verification-requests").then((r) => setRequests(r.data)).catch(() => {});
  }

  useEffect(load, [tenantId]);

  async function createRequest() {
    setError(null);
    setCreating(true);
    try {
      const response = await client.post("/api/v1/verification-requests", { policyId });
      setQrCode(response.data.qrCodePngBase64);
      setRequestUri(response.data.oid4vpRequestUri);
      load();
      setJustCreated(true);
    } catch {
      setError("Could not create verification request - check you have a published policy.");
    } finally {
      setCreating(false);
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage verification requests.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Verification Requests</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="flex gap-2 mb-4">
        <select className="border rounded px-2 py-1" value={policyId} onChange={(e) => setPolicyId(e.target.value)}>
          <option value="">Select policy…</option>
          {policies.map((p) => (
            <option key={p.id} value={p.id}>
              {p.name}
            </option>
          ))}
        </select>
        <button className="btn-primary" onClick={createRequest} disabled={!policyId || creating}>
          {creating ? (
            <span className="inline-flex items-center gap-2">
              <span
                className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"
                aria-hidden="true"
              />
              Creating…
            </span>
          ) : (
            "Create OID4VP request"
          )}
        </button>
      </div>
      {qrCode && (
        <div className="bg-white shadow rounded p-4 mb-6 max-w-md">
          <p className="text-sm text-gray-500 mb-2">Scan with the wallet app:</p>
          <img src={`data:image/png;base64,${qrCode}`} alt="OID4VP QR code" className="w-48 h-48 mb-3" />
          <p className="text-sm text-gray-500 mb-1">
            Or paste this into the wallet's "Enter code manually" (Scan tab):
          </p>
          <input
            readOnly
            className="border rounded px-2 py-1 w-full font-mono text-xs"
            value={requestUri ?? ""}
            onFocus={(e) => e.target.select()}
          />
        </div>
      )}
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Status</th>
            <th className="p-2">Reason</th>
            <th className="p-2">Created</th>
            <th className="p-2">Expires</th>
          </tr>
        </thead>
        <tbody>
          {requests.map((r) => (
            <tr key={r.id} className="border-b text-sm">
              <td className="p-2">{r.status}</td>
              <td className="p-2">{r.statusReason ?? "—"}</td>
              <td className="p-2">{new Date(r.createdAt).toLocaleString()}</td>
              <td className="p-2">{new Date(r.expiresAt).toLocaleString()}</td>
            </tr>
          ))}
        </tbody>
      </table>

      {justCreated && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setJustCreated(false)}>
          <div className="bg-white rounded shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-green-100 text-green-600 text-lg">✓</span>
              <h3 className="text-lg font-semibold">Verification request created</h3>
            </div>
            <p className="text-sm text-gray-600 mb-4">
              The request is ready — scan the QR code or paste the link into the wallet app. It stays visible below
              after you close this.
            </p>
            <div className="flex justify-end">
              <button className="btn-primary" onClick={() => setJustCreated(false)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
