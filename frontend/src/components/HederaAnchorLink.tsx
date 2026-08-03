import { useEffect, useState } from "react";
import type { AxiosInstance } from "axios";

interface AnchorResponse {
  id: string;
  status: "CONFIRMED" | "FAILED" | "DISABLED";
  transactionId: string | null;
  topicSequenceNumber: number | null;
  failureReason: string | null;
}

interface NetworkResponse {
  networkName: string;
  active: boolean;
}

let cachedNetwork: string | null = null;

/** Resolves once per page load - every anchor link on a page shares the same Hedera network. */
async function resolveNetwork(client: AxiosInstance): Promise<string> {
  if (cachedNetwork) return cachedNetwork;
  try {
    const { data } = await client.get<NetworkResponse[]>("/api/v1/hedera/networks");
    cachedNetwork = data.find((n) => n.active)?.networkName ?? "testnet";
  } catch {
    cachedNetwork = "testnet";
  }
  return cachedNetwork;
}

/**
 * Every credential/schema/DID/trusted-issuer mutation anchors a content hash to Hedera Consensus
 * Service (see backend/hedera's HederaAnchorPublisher callers) - this surfaces that anchor's
 * on-chain proof as a HashScan link so a user can verify the hash independently of this app,
 * rather than taking the "anchored" claim on faith. Renders nothing while loading to avoid
 * layout flicker on pages listing many rows at once.
 */
export function HederaAnchorLink({ api, resourceType, resourceId }: { api: AxiosInstance; resourceType: string; resourceId: string }) {
  const [anchor, setAnchor] = useState<AnchorResponse | null | undefined>(undefined);
  const [network, setNetwork] = useState("testnet");

  useEffect(() => {
    let cancelled = false;
    api
      .get<AnchorResponse[]>("/api/v1/hedera/anchors", { params: { resourceType, resourceId } })
      .then((r) => {
        if (cancelled) return;
        const latest = [...r.data].sort((a, b) => (a.id > b.id ? -1 : 1))[0] ?? null;
        setAnchor(latest);
      })
      .catch(() => !cancelled && setAnchor(null));
    resolveNetwork(api).then((n) => !cancelled && setNetwork(n));
    return () => {
      cancelled = true;
    };
  }, [api, resourceType, resourceId]);

  if (anchor === undefined) return null;
  if (!anchor || anchor.status === "DISABLED") {
    return <span className="text-gray-400 text-xs">Not anchored</span>;
  }
  if (anchor.status === "FAILED") {
    return (
      <span className="text-red-600 text-xs" title={anchor.failureReason ?? undefined}>
        Anchor failed
      </span>
    );
  }
  return (
    <a
      href={`https://hashscan.io/${network}/tx/${anchor.transactionId}`}
      target="_blank"
      rel="noreferrer"
      className="text-indigo-600 hover:underline text-xs"
    >
      View on HashScan ↗
    </a>
  );
}
