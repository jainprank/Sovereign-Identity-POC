import { useEffect, useMemo, useState } from "react";
import { useAuth } from "react-oidc-context";
import { createApiClient } from "../lib/api";
import { useTenant } from "../tenant/TenantContext";
import {
  claimCredential, ensureMyWallet, extractPreAuthorizedCode, listCredentials, listPresentations,
} from "./api/walletApi";
import { listNotifications, unreadNotificationCount } from "./api/notificationApi";
import type { NotificationResponse, PresentationResponse, WalletCredentialResponse, WalletResponse } from "./api/types";
import { TabBar } from "./components/TabBar";
import { anchorForCredential, anchorForPresentation, titleFor } from "./lib/credentialDisplay";
import { parseOid4vpUri } from "./lib/oid4vp";
import { AddCredentialOverlay } from "./overlays/AddCredentialOverlay";
import { ConsentOverlay } from "./overlays/ConsentOverlay";
import { CredentialDetail } from "./overlays/CredentialDetail";
import { NotificationsOverlay } from "./overlays/NotificationsOverlay";
import { ScannerOverlay } from "./overlays/ScannerOverlay";
import { SuccessOverlay } from "./overlays/SuccessOverlay";
import { ActivityScreen } from "./screens/ActivityScreen";
import { IdentityScreen } from "./screens/IdentityScreen";
import { WalletScreen } from "./screens/WalletScreen";
import type { ActivityEvent, OverlayState, SuccessInfo, Tab } from "./uiTypes";
import "./styles/wallet.css";

/**
 * Root state machine for the holder-facing mobile wallet (EPIC 8/10/11's UI). Ports the design
 * mock's `app.jsx` tab/overlay/success/toast model, but every piece of data comes from the real
 * backend — see api/walletApi.ts and lib/credentialDisplay.ts for exactly what is and isn't
 * honestly derivable from a HOLDER-scoped session.
 */
export function WalletApp() {
  const auth = useAuth();
  const { tenantId, setTenantId } = useTenant();
  const api = useMemo(() => createApiClient(auth.user?.access_token, tenantId), [auth.user?.access_token, tenantId]);

  const [wallet, setWallet] = useState<WalletResponse | null>(null);
  const [walletLoading, setWalletLoading] = useState(true);
  const [walletError, setWalletError] = useState<string | null>(null);

  const [credentials, setCredentials] = useState<WalletCredentialResponse[]>([]);
  const [presentations, setPresentations] = useState<PresentationResponse[]>([]);

  const [notifications, setNotifications] = useState<NotificationResponse[]>([]);
  const [notifLoading, setNotifLoading] = useState(false);
  const [notifError, setNotifError] = useState<string | null>(null);
  const [unreadCount, setUnreadCount] = useState(0);

  const [tab, setTab] = useState<Tab>("wallet");
  const [overlay, setOverlay] = useState<OverlayState>(null);
  const [success, setSuccess] = useState<SuccessInfo | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 2600);
    return () => clearTimeout(t);
  }, [toast]);

  async function refreshWalletData(walletId: string) {
    const [creds, pres] = await Promise.all([listCredentials(api, walletId), listPresentations(api, walletId)]);
    setCredentials(creds);
    setPresentations(pres);
  }

  async function openNotifications() {
    setOverlay({ t: "notifications" });
    setNotifLoading(true);
    setNotifError(null);
    try {
      setNotifications(await listNotifications(api));
    } catch {
      setNotifError("Couldn't load notifications.");
    } finally {
      setNotifLoading(false);
    }
  }

  useEffect(() => {
    if (!tenantId || !auth.user?.access_token) {
      setWalletLoading(false);
      return;
    }
    let cancelled = false;
    (async () => {
      setWalletLoading(true);
      setWalletError(null);
      try {
        const w = await ensureMyWallet(api);
        if (cancelled) return;
        setWallet(w);
        await refreshWalletData(w.id);
        unreadNotificationCount(api).then((n) => { if (!cancelled) setUnreadCount(n); }).catch(() => {});
      } catch {
        if (!cancelled) setWalletError("Couldn't load your wallet. Please try again.");
      } finally {
        if (!cancelled) setWalletLoading(false);
      }
    })();
    return () => { cancelled = true; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId, auth.user?.access_token]);

  const holderInitials = (auth.user?.profile.email ?? "ID").slice(0, 2).toUpperCase();

  const activityEvents: ActivityEvent[] = useMemo(() => {
    const issueEvents: ActivityEvent[] = credentials.map((wc) => ({
      id: `issue-${wc.id}`,
      kind: "issue",
      icon: "doc",
      cls: "ico-issue",
      title: `Received ${titleFor(wc)}`,
      desc: "Redeemed via OID4VCI",
      time: wc.receivedAt,
      anchor: anchorForCredential(wc.credential),
    }));
    const shareEvents: ActivityEvent[] = presentations.map((p) => {
      const wc = credentials.find((c) => c.id === p.walletCredentialId);
      return {
        id: `share-${p.id}`,
        kind: "share",
        icon: "share",
        cls: "ico-share",
        title: `Shared ${wc ? titleFor(wc) : "a credential"}`,
        desc: `Presented to ${p.audience}`,
        time: p.presentedAt,
        anchor: anchorForPresentation(p),
      };
    });
    return [...issueEvents, ...shareEvents].sort((a, b) => new Date(b.time).getTime() - new Date(a.time).getTime());
  }, [credentials, presentations]);

  if (!tenantId) {
    return (
      <div className="idt">
        <div className="idt-view" style={{ display: "flex", flexDirection: "column", justifyContent: "center", padding: 28 }}>
          <h1 className="idt-title" style={{ marginBottom: 6 }}>ID Trust</h1>
          <p style={{ color: "var(--muted)", fontSize: 13.5, marginBottom: 18 }}>
            Enter your organization's tenant ID to open your wallet.
          </p>
          <input
            placeholder="Tenant ID (UUID)"
            style={{ height: 48, padding: "0 14px", border: "1px solid var(--line)", borderRadius: 8, fontSize: 14 }}
            onBlur={(e) => e.target.value.trim() && setTenantId(e.target.value.trim())}
          />
        </div>
      </div>
    );
  }

  const closeOverlay = () => setOverlay(null);

  const handleScanDetect = async (text: string, purpose: "claim" | "present") => {
    if (purpose === "claim") {
      const code = extractPreAuthorizedCode(text);
      if (!code || !wallet) {
        setToast("Unrecognized offer code.");
        setOverlay(null);
        return;
      }
      try {
        const wc = await claimCredential(api, wallet.id, code);
        await refreshWalletData(wallet.id);
        setOverlay(null);
        setSuccess({
          heading: "Credential added",
          detail: `${titleFor(wc)} has been added to your wallet.`,
          anchor: anchorForCredential(wc.credential),
        });
      } catch {
        setToast("Couldn't redeem that offer.");
        setOverlay(null);
      }
      return;
    }

    const parsed = parseOid4vpUri(text);
    if (!parsed) {
      setToast("Unrecognized QR code.");
      setOverlay(null);
      return;
    }
    setOverlay({ t: "consent", request: parsed });
  };

  return (
    <div className="idt">
      {tab === "wallet" && wallet && (
        <WalletScreen
          holderInitials={holderInitials}
          did={wallet.did}
          credentials={credentials}
          loading={walletLoading}
          error={walletError}
          unreadCount={unreadCount}
          onOpenCred={(id) => setOverlay({ t: "cred", walletCredentialId: id })}
          onAdd={() => setOverlay({ t: "add" })}
          onOpenNotifications={openNotifications}
        />
      )}
      {tab === "wallet" && !wallet && (walletLoading ? (
        <div className="idt-loading">Loading your wallet…</div>
      ) : (
        <div className="idt-error">{walletError ?? "Wallet unavailable."}</div>
      ))}
      {tab === "activity" && <ActivityScreen events={activityEvents} loading={walletLoading} error={walletError} />}
      {tab === "identity" && (
        <IdentityScreen
          holderInitials={holderInitials}
          wallet={wallet}
          credentialCount={credentials.length}
          shareCount={presentations.length}
          issueCount={credentials.length}
          onSignOut={() => auth.removeUser()}
        />
      )}

      <TabBar tab={tab} onTab={setTab} onScan={() => setOverlay({ t: "scan", purpose: "present" })} />

      {overlay?.t === "cred" && wallet && (() => {
        const wc = credentials.find((c) => c.id === overlay.walletCredentialId);
        return wc ? (
          <CredentialDetail
            api={api}
            walletId={wallet.id}
            wc={wc}
            onClose={closeOverlay}
            onPresent={() => setOverlay({ t: "scan", purpose: "present" })}
            onUpdated={(fresh) => setCredentials((prev) => prev.map((c) => (c.id === fresh.id ? fresh : c)))}
            onDeleted={(id) => setCredentials((prev) => prev.filter((c) => c.id !== id))}
          />
        ) : null;
      })()}

      {overlay?.t === "add" && (
        <AddCredentialOverlay
          onClose={closeOverlay}
          onScan={() => setOverlay({ t: "scan", purpose: "claim" })}
          onSubmit={async (input) => {
            if (!wallet) return;
            const code = extractPreAuthorizedCode(input);
            if (!code) throw new Error("No code found");
            const wc = await claimCredential(api, wallet.id, code);
            await refreshWalletData(wallet.id);
            setOverlay(null);
            setSuccess({
              heading: "Credential added",
              detail: `${titleFor(wc)} has been added to your wallet.`,
              anchor: anchorForCredential(wc.credential),
            });
          }}
        />
      )}

      {overlay?.t === "scan" && (
        <ScannerOverlay
          purpose={overlay.purpose}
          onClose={closeOverlay}
          onDetect={(text) => handleScanDetect(text, overlay.purpose)}
        />
      )}

      {overlay?.t === "consent" && wallet && (
        <ConsentOverlay
          api={api}
          walletId={wallet.id}
          request={overlay.request}
          credentials={credentials}
          onClose={closeOverlay}
          onComplete={async (presentation, wc) => {
            await refreshWalletData(wallet.id);
            setOverlay(null);
            setSuccess({
              heading: "Credential shared",
              detail: `${titleFor(wc)} was securely shared with the verifier.`,
              anchor: anchorForPresentation(presentation),
            });
          }}
        />
      )}

      {overlay?.t === "notifications" && (
        <NotificationsOverlay
          api={api}
          notifications={notifications}
          loading={notifLoading}
          error={notifError}
          onClose={closeOverlay}
          onRead={(id) => {
            setNotifications((prev) => prev.map((n) => (n.id === id ? { ...n, read: true } : n)));
            setUnreadCount((prev) => Math.max(0, prev - 1));
          }}
        />
      )}

      {success && <SuccessOverlay info={success} onDone={() => setSuccess(null)} />}

      {toast && (
        <div className="idt-toast">
          <span>{toast}</span>
        </div>
      )}
    </div>
  );
}
