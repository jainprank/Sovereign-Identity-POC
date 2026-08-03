// Parses the real OID4VP request URI produced by backend/verification's
// Oid4vpRequestGenerator#buildUri: `openid4vp://?request=<url-encoded JSON>` where the JSON is
// `{client_id, response_uri, response_type, nonce, presentation_definition}` and
// `presentation_definition` is VerificationService#buildPresentationDefinition's
// `{schemaId, requiredClaims}` — not a DIF Presentation Exchange object, so no PEX parsing here.

export interface ParsedVpRequest {
  clientId: string;
  responseUri: string;
  nonce: string;
  schemaId: string | null;
  requiredClaims: string[];
}

export function parseOid4vpUri(raw: string): ParsedVpRequest | null {
  const text = raw.trim();
  if (!text) return null;

  let requestParam: string | null = null;
  try {
    const url = new URL(text);
    requestParam = url.searchParams.get("request");
  } catch {
    // Not a full URI — maybe the QR only carried the bare JSON or the query string itself.
  }

  let jsonText: string | null = requestParam;
  if (!jsonText) {
    const match = text.match(/request=([^&]+)/);
    if (match) {
      jsonText = decodeURIComponent(match[1]);
    } else if (text.trim().startsWith("{")) {
      jsonText = text;
    }
  }
  if (!jsonText) return null;

  try {
    const parsed = JSON.parse(jsonText);
    const pd = parsed.presentation_definition ?? {};
    if (typeof parsed.client_id !== "string" || typeof parsed.response_uri !== "string" || typeof parsed.nonce !== "string") {
      return null;
    }
    return {
      clientId: parsed.client_id,
      responseUri: parsed.response_uri,
      nonce: parsed.nonce,
      schemaId: typeof pd.schemaId === "string" ? pd.schemaId : null,
      requiredClaims: Array.isArray(pd.requiredClaims) ? pd.requiredClaims.filter((c: unknown) => typeof c === "string") : [],
    };
  } catch {
    return null;
  }
}
