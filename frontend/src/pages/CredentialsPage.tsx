import { useEffect, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import type { PageResponse } from "../lib/pageResponse";
import { HederaAnchorLink } from "../components/HederaAnchorLink";

interface SchemaResponse {
  id: string;
  name: string;
  version: string;
  status: string;
}

interface CredentialResponse {
  id: string;
  subjectDid: string;
  status: string;
  issuedAt: string;
}

interface OfferResponse {
  preAuthorizedCode: string;
  credentialOfferUri: string;
  qrCodePngBase64: string;
  expiresAt: string;
}

interface IssueResponse {
  credential: CredentialResponse;
}

export function CredentialsPage() {
  const auth = useAuth();
  const { tenantId } = useTenant();
  const client = createApiClient(auth.user?.access_token, tenantId);

  const [schemas, setSchemas] = useState<SchemaResponse[]>([]);
  const [credentials, setCredentials] = useState<CredentialResponse[]>([]);
  const [schemaId, setSchemaId] = useState("");
  const [issuerDid, setIssuerDid] = useState("");
  const [subjectDid, setSubjectDid] = useState("");
  const [claimsJson, setClaimsJson] = useState('{\n  "name": "Alice"\n}');
  const [error, setError] = useState<string | null>(null);
  const [issuing, setIssuing] = useState(false);
  const [issued, setIssued] = useState<CredentialResponse | null>(null);

  const [offerCredentialId, setOfferCredentialId] = useState<string | null>(null);
  const [offer, setOffer] = useState<OfferResponse | null>(null);
  const [offerLoading, setOfferLoading] = useState(false);
  const [offerError, setOfferError] = useState<string | null>(null);

  function load() {
    if (!tenantId) return;
    client
      .get<PageResponse<SchemaResponse>>("/api/v1/credential-schemas")
      .then((r) => setSchemas(r.data.content))
      .catch(() => {});
    client
      .get<PageResponse<CredentialResponse>>("/api/v1/credentials")
      .then((r) => setCredentials(r.data.content))
      .catch(() => {});
  }

  useEffect(load, [tenantId]);

  async function issue() {
    setError(null);
    let claims: Record<string, unknown>;
    try {
      claims = JSON.parse(claimsJson);
    } catch {
      setError("Claims must be valid JSON.");
      return;
    }
    setIssuing(true);
    try {
      const response = await client.post<IssueResponse>("/api/v1/credentials", {
        schemaId,
        issuerDid,
        subjectDid: subjectDid.trim() || null,
        claims,
      });
      load();
      setIssued(response.data.credential);
    } catch {
      setError("Could not issue credential - check the schema is PUBLISHED and the issuer DID is valid.");
    } finally {
      setIssuing(false);
    }
  }

  async function generateOffer(credentialId: string) {
    setOfferCredentialId(credentialId);
    setOffer(null);
    setOfferError(null);
    setOfferLoading(true);
    try {
      const response = await client.post<OfferResponse>("/api/v1/oid4vci/offers", { credentialId });
      setOffer(response.data);
    } catch {
      setOfferError("Could not generate an offer for this credential.");
    } finally {
      setOfferLoading(false);
    }
  }

  if (!tenantId) {
    return <p className="text-gray-600">Enter a Tenant ID above to manage credentials.</p>;
  }

  return (
    <div>
      <h2 className="text-lg font-semibold mb-4">Credentials</h2>
      {error && <p className="text-red-600 mb-2">{error}</p>}
      <div className="bg-white shadow rounded p-4 mb-6 grid grid-cols-2 gap-3 max-w-2xl">
        <select className="border rounded px-2 py-1 col-span-2" value={schemaId} onChange={(e) => setSchemaId(e.target.value)}>
          <option value="">Select schema…</option>
          {schemas.map((s) => (
            <option key={s.id} value={s.id}>
              {s.name} v{s.version} ({s.status})
            </option>
          ))}
        </select>
        <input
          className="border rounded px-2 py-1"
          placeholder="Issuer DID"
          value={issuerDid}
          onChange={(e) => setIssuerDid(e.target.value)}
        />
        <input
          className="border rounded px-2 py-1"
          placeholder="Subject DID (optional)"
          value={subjectDid}
          onChange={(e) => setSubjectDid(e.target.value)}
        />
        <textarea
          className="border rounded px-2 py-1 col-span-2 font-mono text-sm"
          rows={5}
          value={claimsJson}
          onChange={(e) => setClaimsJson(e.target.value)}
        />
        <button className="btn-primary col-span-2" onClick={issue} disabled={!schemaId || !issuerDid || issuing}>
          {issuing ? (
            <span className="inline-flex items-center gap-2">
              <span
                className="inline-block h-3.5 w-3.5 animate-spin rounded-full border-2 border-white border-t-transparent"
                aria-hidden="true"
              />
              Issuing credential…
            </span>
          ) : (
            "Issue credential"
          )}
        </button>
      </div>
      <table className="w-full bg-white shadow rounded">
        <thead>
          <tr className="text-left text-sm text-gray-500 border-b">
            <th className="p-2">Subject DID</th>
            <th className="p-2">Status</th>
            <th className="p-2">Issued</th>
            <th className="p-2">Hedera anchor</th>
            <th className="p-2"></th>
          </tr>
        </thead>
        <tbody>
          {credentials.map((c) => (
            <tr key={c.id} className="border-b text-sm">
              <td className="p-2 font-mono">{c.subjectDid ?? "—"}</td>
              <td className="p-2">{c.status}</td>
              <td className="p-2">{new Date(c.issuedAt).toLocaleString()}</td>
              <td className="p-2">
                <HederaAnchorLink api={client} resourceType="CREDENTIAL" resourceId={c.id} />
              </td>
              <td className="p-2 text-right">
                <button
                  className="text-indigo-600 hover:underline"
                  onClick={() => generateOffer(c.id)}
                  disabled={offerLoading && offerCredentialId === c.id}
                >
                  {offerLoading && offerCredentialId === c.id ? "Generating…" : "Generate offer"}
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>

      {offerCredentialId && (
        <div className="bg-white shadow rounded p-4 mt-6 max-w-md">
          <h3 className="font-semibold mb-2">Claimable offer</h3>
          {offerError && <p className="text-red-600 mb-2">{offerError}</p>}
          {offer && (
            <>
              <img
                src={`data:image/png;base64,${offer.qrCodePngBase64}`}
                alt="OID4VCI offer QR code"
                className="w-48 h-48 mb-3"
              />
              <p className="text-sm text-gray-500 mb-1">
                Scan with the wallet app, or paste this code into "Enter code manually":
              </p>
              <input
                readOnly
                className="border rounded px-2 py-1 w-full font-mono text-xs mb-2"
                value={offer.preAuthorizedCode}
                onFocus={(e) => e.target.select()}
              />
              <p className="text-xs text-gray-500">Expires {new Date(offer.expiresAt).toLocaleTimeString()}</p>
            </>
          )}
        </div>
      )}

      {issued && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40" onClick={() => setIssued(null)}>
          <div className="bg-white rounded shadow-lg p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
            <div className="flex items-center gap-3 mb-3">
              <span className="flex h-9 w-9 items-center justify-center rounded-full bg-green-100 text-green-600 text-lg">✓</span>
              <h3 className="text-lg font-semibold">Credential issued</h3>
            </div>
            <p className="text-sm text-gray-600 mb-3">
              The credential has been signed, anchored to Hedera, and now appears in the table below.
            </p>
            <div className="bg-gray-50 rounded p-2 mb-4 font-mono text-xs break-all">
              {issued.subjectDid || "(no subject — claimable by whoever redeems the offer)"}
            </div>
            <div className="flex items-center justify-between">
              <HederaAnchorLink api={client} resourceType="CREDENTIAL" resourceId={issued.id} />
              <button className="btn-primary" onClick={() => setIssued(null)}>
                Done
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
