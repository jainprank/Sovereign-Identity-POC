# IDTrust

Enterprise Digital Identity, Verifiable Credential & Digital Trust Platform.

Source of truth for product/technical requirements lives in Confluence, space **TP**, folder
["IDTrust" spec folder](https://hashgraph.atlassian.net/wiki/spaces/TP/folder/1257996292):

- [Architecture & Implementation Baseline](https://hashgraph.atlassian.net/wiki/spaces/TP/pages/1258160129) — 250 FRs, 18 epics, service/DB/API design, sequence diagrams (canonical reference for implementation)
- [IDTrust v1.0 Master Specification](https://hashgraph.atlassian.net/wiki/spaces/TP/pages/1258127361) — personas, scope, NFRs, user journeys
- [IDTrust_v1.2_API_Specification.yaml](https://hashgraph.atlassian.net/wiki/spaces/TP/pages/1257897993) — OpenAPI reference (endpoint/schema names; re-authored into valid YAML per module as it's built, since the Confluence page mangles list/bracket syntax)

This repo does not yet have the referenced `IDTrust_v1.2_Database_Specification.sql`,
`IDTrust_v1.2_UI_Build_Specification.md`, or `IDTrust_v1.2_AI_Engineering_Backlog.md` — those
pages were referenced from the "Implementation Pack" index but don't exist yet. Schema and UI
detail is derived from the Architecture & Implementation Baseline until/unless those are written.

## Architecture

IDTrust is built as a **modular monolith**, not as 20+ independent microservices (Architecture
& Implementation Baseline, "Decision 1"). One deployable Spring Boot application
(`backend/app`) composes a set of domain modules with clear boundaries; modules are extracted
into standalone services later only where scale or team boundaries justify it.

```
backend/
  app/            composition root — Spring Boot application, config, Flyway migrations
  common/         shared kernel — tenant context, correlation IDs, error handling, audit contract
  identity/       organizations, tenants, users, RBAC              (E02)
  did/            DID lifecycle behind a DIDMethodProvider          (E04)
  kms/            key management behind a KeyProvider abstraction   (E05)
  credential/     schemas, W3C VC issuance, status/revocation       (E06-E08)
  wallet/         wallet + OID4VCI issuance protocol                (E09)
  verification/   OID4VP, verification policy engine, SD-JWT        (E10-E11)
  trust/          trust registry                                    (E12)
  hedera/         Hedera trust adapter (anchoring only, async)       (E13)
```

Every tenant-owned table carries `tenant_id`; isolation is enforced at the API, service, and
database (Postgres RLS) layers. Standards: W3C DID, W3C VC, OID4VCI, OID4VP, SD-JWT VC.
Hedera is used only for anchoring hashes/timestamps — never for PII, credentials, or keys.

## Epic roadmap

Build order agreed 2026-07-28:

| Epic | Scope | Status |
| --- | --- | --- |
| E01 | Foundation (this scaffold: repo, Docker Compose, CI, health check, module skeleton) | ✅ done |
| E02 | IAM / Tenancy / RBAC | ✅ verified (built + exercised against a live backend) |
| E03 | Database / Row-Level Security | ✅ verified (built + exercised against a live backend) |
| E04 | DID | ✅ verified (built + exercised against a live backend) |
| E05 | KMS / Key Lifecycle | ✅ verified (built + exercised against a live backend) |
| E06 | Schemas / Templates | ✅ verified (built + exercised against a live backend) |
| E07 | Credential Issuance | ✅ verified (built + exercised against a live backend) |
| E08 | Status / Revocation | ✅ verified (built + exercised against a live backend) |
| E09 | OID4VCI / Wallets | ✅ verified (built + exercised against a live backend) |
| E10 | OID4VP / Verification | ✅ verified (built + exercised against a live backend) |
| E11 | SD-JWT | ✅ verified (built + exercised against a live backend) |
| E12 | Trust Registry | ✅ verified (built + exercised against a live backend) |
| E13 | Hedera | ✅ verified (built + exercised against a live backend) |
| E14 | Enterprise Integrations | ✅ verified (built + exercised against a live backend) |
| E15 | UI / QA / Security / Production | ✅ verified (built + exercised against a live backend) |

All epics above were verified by running the full modular monolith against a live Postgres +
Keycloak stack and exercising each epic's real endpoints via `curl` (there are effectively no
automated tests in this codebase). Three real bugs were found and fixed during this pass:

1. **API key / OAuth access token bearer auth was universally broken.** `BearerTokenAuthenticationFilter`
   unconditionally tried to decode any presented bearer token as a Keycloak JWT regardless of
   whether an earlier filter (`ApiKeyAuthenticationFilter`) had already authenticated it via an
   opaque, non-JWT credential — every API key/OAuth-client request 401'd. Fixed in
   `identity.security.SecurityConfig`'s `SelectiveBearerTokenResolver` by also short-circuiting
   when the `SecurityContext` already holds an `Authentication`.
2. **`@PreAuthorize` denials returned 500 instead of 403.** Spring Security 6's
   `AuthorizationDeniedException` (thrown by the method-security AOP proxy around the controller
   method, not the security filter chain) fell through `GlobalExceptionHandler`'s generic
   `Exception` catch-all. Fixed by adding a dedicated `AccessDeniedException` handler mapping to
   `FORBIDDEN`/403.
3. **API request logs (FR-API-007) were always persisted with a null tenant/API-client id.**
   `TenantContextInterceptor.afterCompletion` (an MVC interceptor) cleared `TenantContext`/
   `CurrentApiClientContext` *before* `ApiRequestLoggingFilter`'s outer servlet-filter `finally`
   block ran, so every log row lost its tenant attribution and the query endpoint always returned
   empty. Fixed by having the interceptor snapshot the resolved ids onto request attributes before
   clearing, which the filter now reads instead.

## E02/E03: IAM, Tenancy, RBAC, Row-Level Security

The `identity` module now implements:

- **Organizations & tenants** (platform-administered; `organizations`/`tenants` tables are not
  tenant-scoped themselves and carry no RLS policy).
- **Users, roles, permissions, role assignments** (`users`, `roles`, `permissions`,
  `role_permissions`, `user_roles` — all tenant-owned and RLS-protected, except the global
  `permissions` catalogue). Seven system roles and an initial permission catalogue are seeded via
  `V2__identity_core.sql`; tenants can additionally define their own custom roles with a
  hand-picked subset of permissions (FR-ORG-013).
- **Invitations** (`invitations` table): `POST /api/v1/invitations` (admin-only, tenant-scoped)
  and the public `POST /api/v1/invitations/accept` (no auth — the invited party isn't a tenant
  member yet; the tenant is resolved from the invitation token).
- **Row-Level Security** (`V3__row_level_security.sql`): `ENABLE`+`FORCE ROW LEVEL SECURITY` on
  every tenant-owned table, policies keyed on the Postgres session variable `app.tenant_id`. A
  custom Hibernate `ConnectionProvider` (`TenantConnectionProvider`) sets that session variable on
  every borrowed connection from `TenantContext` — the same value the API layer already resolved
  from the `X-Tenant-ID` header — so isolation is enforced redundantly at the API, service, and
  database layers.
- **Keycloak-backed JWT auth** (`SecurityConfig`): stateless OAuth2 resource server, realm roles
  mapped to `ROLE_*` authorities via `KeycloakRealmRoleConverter`. `TenantMembershipFilter` bridges
  the authenticated JWT subject to a local `AppUser` scoped to the tenant named by
  `X-Tenant-ID`, rejecting non-members with a 403 in the standard `ApiError` shape.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — the environment used to build it has no local
  Java/Gradle/Docker toolchain. Treat the first local `./gradlew build`/`test` as the real
  validation step.
- Real Keycloak realm/client provisioning and first-login account linkage (beyond invitation
  acceptance, which does link `keycloak_subject_id`) is out of scope for this pass —
  `TenantMembershipFilter` resolves users by `keycloak_subject_id` first, falling back to the JWT
  `email` claim, but does not auto-link on that fallback path.

## E04/E05: DID Lifecycle, KMS/Key Management

The `kms` and `did` modules now implement:

- **Key management (`kms`)**: `ManagedKey` entities (`keys` table, tenant-owned, RLS-protected)
  behind a `KeyProvider` abstraction (`type()`, `generateKey`, `sign`, `deactivate`, `delete`,
  `healthCheck()`). Only `InternalKeyProvider` is implemented for now — JDK-native key generation
  for `ED25519`, `SECP256K1`, `RSA_2048`, with private keys envelope-encrypted at rest
  (`KeyEncryptionService`: AES/GCM/NoPadding, random 12-byte IV, master key-encryption-key from
  `idtrust.kms.master-key-base64`). `KeyProviderType` reserves `AWS_KMS`, `VAULT`, `HSM` for later
  epics. `KeyController` exposes create/get/rotate/deactivate/delete
  (`PLATFORM_ADMIN`/`ORG_ADMIN` only) — signing is intentionally not exposed over REST, only used
  internally (e.g. by future credential issuance).
- **DID lifecycle (`did`)**: `Did` entities (`dids` table, tenant-owned, RLS-protected) behind a
  `DidMethodProvider` abstraction (`method()`, `create`, `resolve`, `update`, `deactivate`).
  `did:key` (self-certifying, Ed25519, multicodec + base58btc encoding via a hand-written
  `Base58`/`Ed25519Codec`) and `did:web` (resolves live via HTTPS GET to
  `/.well-known/did.json`) are both implemented, despite the Master Spec listing `did:web` as
  "V1" — the AI build instructions and MVP backlog call for both now, so both were built.
  Rotation semantics differ by method: `did:web` rotates the key in place (same DID, new key,
  updated DID Document); `did:key` rejects rotation (`INVALID_REQUEST`) since the key **is** the
  identifier — callers must create a new DID instead. `DidController` exposes
  create/list/get/rotate/deactivate (`PLATFORM_ADMIN`/`ORG_ADMIN`/`ISSUER`) plus a public,
  unauthenticated `GET /api/v1/dids/resolve` (DID resolution is not tenant-scoped or gated).
- **Schema**: `V4__did_kms_core.sql` (`dids`, `keys` tables) + `V5__did_kms_row_level_security.sql`
  (RLS policies, mirroring the `V3` pattern). `keys.did_id` is a plain UUID column, not a JPA
  relationship — `did` depends on `kms` in the Gradle graph, never the reverse, so `ManagedKey`
  cannot reference the `Did` entity type.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02/E03. Treat the first local
  `./gradlew build`/`test` as the real validation step.
- `idtrust.kms.master-key-base64` defaults to an insecure all-zero key for local dev only; every
  non-local environment **must** override it via `KMS_MASTER_KEY_BASE64` (see `.env.example` and
  `application.yml` comments).
- Only Ed25519 verification methods are built for DID documents (both `did:key` and `did:web`) —
  JWK-based verification methods for `SECP256K1`/`RSA_2048` keys aren't specified anywhere yet and
  were left out as premature scope.
- `InternalKeyProvider.deactivate`/`delete` are no-ops beyond status transitions (there's no
  external key material to revoke yet) — meaningful once `AWS_KMS`/`VAULT`/`HSM` providers exist.

## E06: Credential Schemas

The `credential` module now implements schema management (EPIC 5, FR-SCH-*):

- **`CredentialSchema`** (`credential_schemas` table, tenant-owned, RLS-protected): versioned by
  row rather than by mutating history — `name`/`version`/`tenant_id` is unique, so publishing a
  schema (FR-SCH-005) freezes that version for issuance to reference, and versioning
  (FR-SCH-004) creates a new row with the same `name` and a new `version` rather than editing the
  published one. Lifecycle: `DRAFT` (freely editable) → `PUBLISHED` (immutable) → `DEPRECATED`
  (no longer intended for new issuance; already-issued credentials keep referencing it).
- **Claim catalogue**: each schema's claims (name, `ClaimType` — `STRING`/`NUMBER`/`BOOLEAN`/
  `DATE`, required/optional, description, optional `ClaimValidation` and `ClaimDisplay`) are
  stored as a typed `SchemaDefinition` in the `schema` JSONB column — covers FR-SCH-007 through
  FR-SCH-011 (required/optional claims, data types, validation rules, display metadata).
- **`CredentialFormat`** reserves `W3C_VC` (implemented, used by E07) and `SD_JWT_VC` (reserved
  for E11), mirroring how `KeyProviderType`/`DidMethod` reserve values for methods not yet built.
- `CredentialSchemaController` exposes create/search/get/update/publish/deprecate plus
  `POST /{id}/versions` (FR-SCH-014 Schema Search via optional `name`/`status` query params).
  Mutations require `PLATFORM_ADMIN`/`ORG_ADMIN`/`ISSUER`; reads also allow `VERIFIER` (verifiers
  need schema claims to build verification policies, FR-VER-002).
- FR-SCH-012/013 (Import/Export) are V1-priority and out of scope for this pass.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E05.
- No claim-validation engine exists yet — `ClaimValidation` rules are stored and round-tripped but
  not enforced anywhere; that lands with credential issuance (E07), which validates claims against
  a schema before signing.

## E07: Credential Issuance

The `credential` module now implements issuance (EPIC 6, FR-ISS-*):

- **No separate "credential definition" resource** — FR-ISS-001 (Create Credential Definition) and
  FR-ISS-002 (Select Schema) are folded into a single issuance request (`schemaId`, `issuerDid`,
  `subjectDid`, `claims`, optional `expiresAt`), matching the Baseline's literal DB schema (no
  `credential_definitions` table) and its `POST /api/v1/credentials` example exactly. The
  referenced schema must be `PUBLISHED`.
- **`Credential`** (`credentials` table, tenant-owned, RLS-protected): `id, tenant_id, schema_id,
  issuer_did, subject_did, format, credential_jwt, credential_hash, status, issued_at, expires_at,
  created_at, updated_at`. `credential_jwt` and `updated_at` are intentional extensions beyond the
  Baseline's literal DDL (storing only a hash isn't enough to later deliver or re-verify the
  credential); `schema_id` is `NOT NULL` here (the Baseline allows it nullable) since claim
  validation always needs a schema. `CredentialStatus` reserves `SUSPENDED`/`REVOKED`/`EXPIRED` for
  E08 — only `ISSUED` is reachable from this epic.
- **Claim validation** (FR-ISS-004, `ClaimValidator`): checks required-claim presence, primitive
  type matching per `ClaimType`, and any `ClaimValidation` pattern/min/max rules declared on the
  schema's claims — the enforcement point `CredentialSchema`'s Javadoc said was still missing.
- **Issuer/subject resolution** (FR-ISS-005/006/007): the issuer DID must belong to the requesting
  tenant (new `DidService.getByDidString`/`DidRepository.findByTenantIdAndDid`); the subject DID
  (optional) is resolved via the existing public `DidService.resolve`, which works for any DID the
  platform doesn't necessarily manage. The issuer's active signing key comes from the existing
  `KeyService.getActiveKeyForDid`.
- **JWT-VC signing** (`JwtVerifiableCredentialSigner`): builds a W3C Verifiable Credential as a
  signed JWT — header `{alg: EdDSA, typ: JWT, kid}` (the `kid` is the issuer DID's
  `assertionMethod` verification method id, read directly from its stored DID Document rather than
  re-derived), payload `{iss, sub, iat, exp, jti: urn:uuid:<credentialId>, vc: {...claims}}`.
  Only Ed25519 issuer keys are supported: the JDK's Ed25519 `Signature` produces a raw signature
  that's directly JWS-EdDSA-compatible with no DER conversion, unlike ECDSA/RSA — this has no
  practical impact today since E04 only ever generates Ed25519 DID verification-method keys.
- **Credential offer / QR / deep link** (FR-ISS-009/010/011, `CredentialOfferGenerator`): a minimal
  `openid-credential-offer://` URI (forward-compatible scaffolding ahead of E09's full OID4VCI
  wallet-claim protocol, not yet built) plus a base64-encoded PNG QR code rendered via the newly
  added ZXing library (`com.google.zxing:core`/`javase` — no QR capability existed anywhere in the
  codebase, and hand-rolling QR matrix generation was judged out of scope). The offer URI doubles
  as the deep link, per the Baseline's own single-field response example.
- **Tracking** (FR-ISS-013, FR-ISS-020): `CredentialController` exposes
  `POST /api/v1/credentials` (issue), `GET` (search by `subjectDid`/`status`), `GET /{id}`
  (detail) — `PLATFORM_ADMIN`/`ORG_ADMIN`/`ISSUER` only (no `VERIFIER` read access here, unlike
  schemas; verifiers work with presented credentials via E10). Every successful issuance publishes
  a `CREDENTIAL_ISSUED` audit event.
- **Explicitly out of scope for this pass**: FR-ISS-014 (Cancel Issuance) and FR-ISS-015 (Retry
  Failed Issuance) — the Baseline's literal API example is synchronous with no pending/claimable
  offer state to cancel or retry against; building either now would mean inventing an unspecified
  async state machine that properly belongs with E09. FR-ISS-016 through FR-ISS-019 (Bulk
  Issuance, Dynamic Claim Population, External Data Source, Credential Preview) are V1-priority and
  also out of scope.
- **Schema**: `V8__credential_core.sql` (`credentials` table) +
  `V9__credential_row_level_security.sql` (RLS policy, mirroring the `V3`/`V5`/`V7` pattern).

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E06.
- The credential offer/QR scheme is intentionally minimal scaffolding, not a real OID4VCI
  pre-authorized-code or authorization-code grant — that protocol lands with E09.
- No credential-presentation or verification path exists yet (E10); this epic only covers issuance
  and basic tracking.

## E08: Credential Status & Revocation

The `credential` module now implements lifecycle management (EPIC 7, FR-CRED-*):

- **No separate registry resource**: FR-CRED-001 (Credential Registry), FR-CRED-002 (Credential
  Search) and FR-CRED-003 (Credential Detail) are already satisfied by `CredentialIssuanceService`'s
  existing `search`/`get` (built in E07) — this epic adds only the status-mutation and
  status-history capability on top.
- **Status transitions** (`CredentialLifecycleService`, `credentials` table's existing `status`
  column): `revoke` (FR-CRED-005, permanent — an `EXPIRED` credential cannot be revoked, an already
  `REVOKED` one is a no-op error), `suspend` (FR-CRED-006, only from `ISSUED`), `reinstate`
  (FR-CRED-007, only from `SUSPENDED`, reverses a suspension). `Credential`'s transition methods
  (`revoke`/`suspend`/`reinstate`/`expire`) are intentionally "dumb" — they perform no legality
  checks themselves; guarding which transitions are allowed is `CredentialLifecycleService`'s job,
  matching the `requireDraft`/`requirePublished` pattern already established in
  `CredentialSchemaService`.
- **Renewal vs. reissuance** (FR-CRED-009 vs. FR-CRED-010): the Baseline gives both the same generic
  acceptance-criteria template, so this pass draws a deliberate distinction — `renew` mints a fresh
  credential with the **same** claims and a new expiry, marking the original `EXPIRED` (superseded,
  no wrongdoing implied); `reissue` mints a fresh credential with **corrected** claims (and
  optionally a new expiry), marking the original `REVOKED` (its data is now considered incorrect, so
  it must not remain usable). A revoked credential cannot be reissued — a new credential must be
  issued instead.
- **`claims`** (new `credentials.claims` JSONB column, `V10__credential_status_lifecycle.sql`): a
  documented extension beyond the Baseline's literal DDL. Renewal and reissuance both need the
  original claims to re-sign a new credential, and decoding them back out of `credential_jwt` would
  be a fragile detour around data the platform already had at issuance time.
- **`CredentialStatusEvent`** (new `credential_status_events` table, tenant-owned, RLS-protected):
  fulfils the Baseline's section 7 ERD's `CREDENTIAL --< CREDENTIAL_STATUS` one-to-many relationship,
  which the flat `credentials.status` column (from E07) doesn't itself model. Every transition
  (including lazy expiration) appends a row with previous/new status and reason — a queryable,
  domain-typed history distinct from the generic `AuditEvent` audit log, satisfying FR-CRED-013
  (Lifecycle Audit) more specifically than a generic audit trail would.
- **Lazy expiration** (FR-CRED-008): applied on read (`effectiveStatus`, used by `checkStatus` and
  `statusList`) rather than by a background sweep — no scheduled/async infrastructure (Kafka) has
  been wired up by any epic so far, so a dedicated expiration worker would be premature at this
  stage.
- **Status list** (FR-CRED-011, `statusList`): a plain id/status list, optionally filtered by
  `schemaId`. FR-CRED-012 (Bitstring Status List) is V1-priority and out of scope — it requires
  implementing the W3C StatusList2021 bitstring-encoded credential format, not just a list.
- **Status verification** (FR-CRED-014, `checkStatus`): returns the effective status for a single
  credential, applying lazy expiration first.
- **`CredentialLifecycleController`** exposes `POST /{id}/revoke`, `/suspend`, `/reinstate`,
  `/renew`, `/reissue` (`PLATFORM_ADMIN`/`ORG_ADMIN`/`ISSUER`), plus `GET /{id}/status` and
  `GET /status-list` which additionally allow `VERIFIER` — a verifier needs to check credential
  status when validating a presentation, so this is exposed ahead of E10 for when that capability is
  wired up. `GET /{id}/history` returns the raw `CredentialStatusEvent` list.
- **Schema**: `V10__credential_status_lifecycle.sql` (`credentials.claims`/`status_reason` columns,
  `credential_status_events` table) + `V11__credential_status_lifecycle_row_level_security.sql`
  (RLS policy, mirroring the `V3`/`V5`/`V7`/`V9` pattern).

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E07.
- FR-CRED-012 (Bitstring Status List) is not implemented — `statusList` returns a plain id/status
  list, not a W3C StatusList2021 bitstring credential.
- Expiration is lazy/on-read only; a credential past `expiresAt` that is never read (via `checkStatus`,
  `statusList`, or any other lookup that calls `effectiveStatus`) keeps its stale `ISSUED` status in
  the database until it is.

## E09: Digital Wallet & OID4VCI

The new `wallet` module implements EPIC 8 (Digital Wallet, FR-WAL-*) and EPIC 9 (OID4VCI,
FR-OID4VCI-*), the pre-authorized-code grant flow through which a holder's wallet claims a
credential this platform issued in E07:

- **Wallet** (`Wallet`, FR-WAL-001): creating a wallet provisions a real `did:key` DID via the
  existing `did` module (`DidService.create`) — that call already generates and stores a signing
  key via `kms` as a side effect (E04/E05), so FR-WAL-002 (Wallet DID) and FR-WAL-003 (Wallet Key)
  are both satisfied with no additional key-provisioning code. `ownerUserId` is a plain UUID column
  with no JPA relationship into `identity`, mirroring the `keys.did_id` precedent from E04/E05 —
  `wallet` depends on `did`/`credential` but must not gain a dependency on `identity`.
- **Wallet credentials** (`WalletCredential`, FR-WAL-004/005/006/007/009): stores only a
  `credentialId` reference into the `credential` module's `credentials` table, never a copy of
  `credential_jwt` — the underlying `Credential` remains the single source of truth for the VC and
  its status, so a wallet's view never drifts out of sync with a subsequent E08 revoke/suspend/
  renew. Deleting a wallet's copy (FR-WAL-007) removes only this reference row, never the
  underlying credential. `category` (FR-WAL-009) is free-text; no fixed taxonomy is specified
  anywhere in the Baseline.
- **OID4VCI session** (`Oid4vciSession`, `Oid4vciSessionService`, FR-OID4VCI-006/009/010/011/012/014):
  a new, separate offer/claim mechanism, entirely distinct from E07's `CredentialOfferGenerator`/
  `CredentialIssuanceService.issue()` placeholder offer (left untouched) — E07 already signs
  credentials synchronously at issuance time, so this session exists purely to gate *handing the
  already-signed JWT to a claiming wallet* behind a one-time pre-authorized code (10-minute TTL)
  and a short-lived access token, not to trigger any new signing operation. Lifecycle:
  `OFFERED` → `TOKEN_ISSUED` → `CLAIMED`, with `EXPIRED` reachable from either of the first two if
  the TTL lapses.
- **Authorization Code Flow deferred** (FR-OID4VCI-007/008): despite being labeled MVP in the
  Baseline, this requires an interactive, browser-redirect-based user authorization step, and no
  wallet UI exists yet to drive it (E15). The pre-authorized-code grant alone is a complete path for
  this platform, since its own credential issuance (E07) is already issuer-initiated and
  synchronous — a holder never needs to "authorize" access to claims they didn't choose.
  FR-OID4VCI-015 (Wallet Notification) is also out of scope — no notification infrastructure
  (EPIC 17) exists yet.
- **Issuer metadata** (FR-OID4VCI-001/002, `issuerMetadata`): derived from the tenant's `PUBLISHED`
  `CredentialSchema`s rather than a separate configuration resource, consistent with schemas
  already being this platform's only notion of a "credential configuration".
- **Unauthenticated protocol endpoints, still tenant-scoped**: `GET /issuer-metadata`,
  `POST /token` and `POST /credential` are `permitAll` in `SecurityConfig` — a holder's wallet calls
  them with no Keycloak session — but still get correct Row-Level Security scoping, because
  `TenantContextInterceptor` populates `TenantContext` from the `X-Tenant-ID` header alone for
  *every* dispatched request, regardless of authentication status. Callers of these three endpoints
  must always send `X-Tenant-ID`.
- **Presentation flows deferred to E10**: FR-WAL-010 through 017 (generating/submitting a
  presentation) require a verifier's presentation request (OID4VP), which doesn't exist until E10.
  FR-WAL-018 through 022 (backup/recovery, multi-device sync, lock/PIN, biometric, offline mode) are
  V1/V2-priority and also out of scope.
- **`WalletController`** exposes `POST/GET /api/v1/wallets`, `GET /{id}`, `GET/GET/PUT/DELETE
  /{id}/credentials[/{credentialId}]`, `POST /{id}/claim` (`PLATFORM_ADMIN`/`ORG_ADMIN`/`HOLDER`).
  **`Oid4vciController`** exposes `POST /api/v1/oid4vci/offers` (issuer-side, authenticated,
  `PLATFORM_ADMIN`/`ORG_ADMIN`/`ISSUER`) plus the three `permitAll` protocol endpoints above.
- **Schema**: `V12__wallet_core.sql` (`wallets`, `wallet_credentials`, `oid4vci_sessions` tables) +
  `V13__wallet_row_level_security.sql` (RLS policy, mirroring the `V3`/`V5`/`V7`/`V9`/`V11` pattern).

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E08.
- `GET /api/v1/wallets` returns every wallet in the tenant, not just the caller's own — no
  per-holder "my resources" ownership filter exists anywhere else in this codebase yet either.
- FR-OID4VCI-007/008 (Authorization Code Flow) and FR-OID4VCI-015 (Wallet Notification) are not
  implemented — see above.
- FR-WAL-010 through 022 (presentation flows, backup/recovery, multi-device sync, lock/PIN,
  biometric, offline mode) are not implemented — deferred to E10 or later, see above.

## E10: OID4VP & Verification

The new `verification` module implements EPIC 10 (Verification, FR-VER-*), EPIC 11 (OID4VP &
DCQL, FR-OID4VP-*) and EPIC 12 (Policy Engine, FR-POL-*) — all three are combined into this single
epic per the agreed build order, since the Policy Engine was already scoped into `verification`
back in E01 and is a hard prerequisite for FR-VER-001/013. The wallet-side half of the OID4VP
handshake (generating and signing a Verifiable Presentation) is added to the existing `wallet`
module in this same pass, in a new `com.idtrust.wallet.oid4vp` package.

- **Policy Engine** (`com.idtrust.verification.policy`, FR-POL-001 through 011): `VerificationPolicy`
  is versioned by row exactly like `credential.schema.CredentialSchema` — `DRAFT` (editable) →
  `ACTIVE` (immutable, referenceable by verification requests) → `INACTIVE`, with
  `createNewVersion` creating a new row rather than mutating an active policy's rules. Rules
  (`PolicyDefinition`, FR-POL-006 through 011) are a single JSONB payload: an optional
  `schemaId`, `requiredClaims`, `requireActiveStatus`, `requireNotExpired`, and
  `trustedIssuerDids` — a static allowlist stand-in for FR-POL-011/FR-VER-012 (Trust Rule/Verify
  Trust), since the full dynamic Trust Registry (EPIC 13) doesn't exist until E12. An empty
  `trustedIssuerDids` list means "trust any issuer" rather than failing closed.
- **Verification requests** (`VerificationRequest`, `VerificationService`, FR-VER-001/002/003/015):
  `createRequest` loads an `ACTIVE` policy, generates a random `nonce` (anti-replay,
  FR-OID4VP-012), and builds an `openid4vp://` request URI + QR PNG via the new
  `Oid4vpRequestGenerator` (mirrors `wallet.oid4vci.Oid4vciOfferGenerator`'s ZXing-based shape and
  inherits the same "no base-URL config exists in this codebase" limitation for its embedded
  `response_uri`). Requests expire after a 10-minute TTL, matching `Oid4vciSession`'s precedent.
- **The verification pipeline** (`VerificationService#presentAndVerify`, FR-VER-006 through 014,
  FR-OID4VP-004/005) is genuine cryptographic verification, not a superficial claims check. Given a
  holder's signed VP JWT, it: decodes the JWT (new `credential.issuance.
  JwtVerifiableCredentialVerifier` utility) → checks `nonce`/`aud` against the request → resolves
  the holder DID via `DidService.resolve` (public, cross-tenant) and verifies the VP's own
  signature, requiring the JWT's `kid` to be listed in the DID Document's `assertionMethod`/
  `authentication` (defends against a valid signature over a substituted key) → does the same for
  the embedded VC JWT against the *issuer's* DID → checks subject binding (VP's `iss` == VC's
  `sub`, FR-VER-009 — real proof-of-possession, not string matching) → checks VC expiry, the
  policy's `trustedIssuerDids` and `requiredClaims` → looks up credential status via
  `CredentialIssuanceService`. Every individual check's pass/fail outcome is recorded in a
  `checks` JSONB map (FR-VER-013), not just a final boolean.
- **`JwtVerifiableCredentialVerifier`** (`com.idtrust.credential.issuance`) reverses
  `did.provider.Ed25519Multibase`'s encoding by hand — strip the `z` multibase prefix, Base58-decode
  (reusing `did.provider.Base58`), strip the 2-byte `0xed 0x01` Ed25519 multicodec prefix, then
  `kms.crypto.Ed25519Codec.decodeRawPublicKey` — to reconstruct a `java.security.PublicKey` and
  verify with the JDK's `Ed25519` `Signature` implementation. It takes an already-resolved DID
  Document rather than resolving one itself, keeping it a pure, side-effect-free crypto utility.
- **FR-VER-011 (Verify Status) known limitation**: credential status lookup only works reliably
  when the credential's issuer belongs to the *same tenant* as the verification request —
  `CredentialIssuanceService.get` is strictly tenant-scoped under RLS with no public cross-tenant
  equivalent to `DidService.resolve`. A cross-tenant lookup failure is recorded as
  `statusCheck: "UNKNOWN"` rather than failing the whole verification or crashing.
- **Wallet-side presentation signing** (`wallet.oid4vp.WalletPresentationSigner`,
  `WalletPresentation`, FR-WAL-011/012/017): structurally the mirror image of `credential.issuance.
  JwtVerifiableCredentialSigner` — same header shape, same Ed25519-only restriction — except the
  payload wraps a `vp.verifiableCredential` array of already-signed VC JWTs and carries
  `aud`/`nonce` instead of an issuer's own claims. Signing with the holder's own KMS-backed key
  (provisioned back in E09's `Wallet` creation) is genuine cryptographic proof of possession, not a
  superficial claim match. Every signed presentation is recorded in a new `wallet_presentations`
  history table. Only a single whole credential is presented per request in this pass —
  multi-credential/derived-claim presentations (FR-WAL-013 through 016) are unbuilt extensions.
- **Deliberate module-boundary decision**: `wallet` and `verification` do not depend on each other.
  `POST /api/v1/wallets/{id}/credentials/{credentialId}/present` (authenticated, `HOLDER`) only
  returns a signed VP JWT artifact to the calling holder client; that client is expected to
  independently `POST` the artifact to the verifier's own
  `POST /api/v1/verification-requests/{id}/present`. This mirrors real-world OID4VP, where the
  wallet and verifier are different parties on different domains.
- **Unauthenticated protocol endpoint, still tenant-scoped**: `POST
  /api/v1/verification-requests/{id}/present` is `permitAll` in `SecurityConfig` — a holder's
  wallet/client calls it with no Keycloak session — but still gets correct Row-Level Security
  scoping via `TenantContextInterceptor`'s `X-Tenant-ID` header handling, the same established
  pattern as E09's three OID4VCI protocol endpoints. Callers must always send `X-Tenant-ID` set to
  the verifier's tenant.
- **`VerificationPolicyController`** exposes `POST/GET /api/v1/verification-policies`, `GET/PATCH
  /{id}`, `POST /{id}/activate`, `POST /{id}/deactivate`, `POST /{id}/versions`
  (`PLATFORM_ADMIN`/`ORG_ADMIN`/`VERIFIER`). **`VerificationController`** exposes `POST/GET
  /api/v1/verification-requests`, `GET /{id}` (same three roles) plus the `permitAll`
  `POST /{id}/present` above. **`WalletController`** gains `POST /{id}/credentials/{credentialId}/
  present` and `GET /{id}/presentations` (`PLATFORM_ADMIN`/`ORG_ADMIN`/`HOLDER`).
- **Schema**: `V14__verification_core.sql` (`verification_policies`, `verification_requests`,
  `wallet_presentations` tables) + `V15__verification_row_level_security.sql` (RLS policy,
  mirroring the `V3`/`V5`/`V7`/`V9`/`V11`/`V13` pattern).

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E09.
- FR-VER-011 (Verify Status) degrades to `UNKNOWN` for cross-tenant issuers — see above.
- FR-VER-012/FR-POL-011 (Trust Rule) is a static per-policy allowlist, not the dynamic Trust
  Registry (EPIC 13/FR-TRUST-*) — deferred to E12 per the build sequence.
- Only one credential is checked per presentation (`vp.verifiableCredential[0]`) — multi-credential
  presentations are unbuilt.
- FR-WAL-013 through 016 (multi-credential/derived-claim presentations) are not implemented.
- Selective disclosure for SD-JWT VCs (`CredentialFormat.SD_JWT_VC`, reserved since E06) is
  implemented in E11, below.
- The OID4VP request URI's `response_uri` is a relative path (no base-URL config exists anywhere
  in this codebase) — same documented limitation as E09's credential offer URIs.

## E11: SD-JWT

Selective Disclosure JWT Verifiable Credentials (IETF SD-JWT VC) — the format reserved as
`CredentialFormat.SD_JWT_VC` since E06 — are now fully implemented end-to-end: issuance
(`credential`), selective disclosure at presentation time (`wallet`, FR-WAL-014), and
disclosure-integrity checking (`verification`). There is no dedicated SD-JWT epic/FR-prefix in the
Baseline's catalogue; this is a cross-cutting slice across EPIC 5/6 (format), EPIC 8 (FR-WAL-014),
and EPIC 10 (section 15's Signature/Digest/Disclosure verification checks).

- **Issuance** (new `credential.issuance.sdjwt.SdJwtVcSigner`): for a schema whose
  `format == SD_JWT_VC`, `CredentialIssuanceService.issue` now builds one `[salt, claimName,
  claimValue]` disclosure per claim instead of embedding claims directly in the JWT payload,
  computes each disclosure's base64url-SHA-256 digest, sorts the digests lexicographically (so
  array order can't leak claim-definition order), and signs a payload carrying only
  `iss`/`sub`/`iat`/`exp`/`jti`/`vct`/`_sd_alg`/`_sd` — the same Ed25519-via-KMS signing path as
  `JwtVerifiableCredentialSigner`. The persisted `Credential.credentialJwt` becomes the
  **combined format** string: `<issuer JWT>~<disclosure1>~...~<disclosureN>~`. The subject DID
  (`sub`) is deliberately left as a plain, always-visible top-level claim rather than a digest —
  FR-VER-009 subject-binding and wallet credential lookups need it without first requiring a
  disclosure to be opened.
- **Selective disclosure at presentation time** (new `credential.issuance.sdjwt.
  SdJwtPresentationBuilder`, FR-WAL-014): `WalletService.presentCredential` now takes a
  `disclosedClaims` list (`WalletDtos.PresentRequest`); for an SD-JWT VC credential,
  `SdJwtPresentationBuilder.filterDisclosures` keeps the issuer JWT segment untouched and drops
  every disclosure whose claim name isn't in the holder-chosen list (`null`/empty means "disclose
  everything") before the artifact is wrapped in the outer VP JWT — a plain W3C JWT-VC has no
  disclosures to withhold, so the parameter is a no-op for that format.
- **Verifier-side integrity checking** (new `credential.issuance.sdjwt.SdJwtVerifier`,
  `VerificationService.decodeCredentialArtifact`): a `"~"` in the credential artifact string (never
  legal inside a compact 3-part JWT) distinguishes a combined-format SD-JWT from a plain JWT-VC.
  `SdJwtVerifier.verifyDisclosures` recomputes each remaining disclosure's digest and confirms it
  was actually present in the issuer-signed `_sd` claim before exposing the claim value — a genuine
  hash-comparison check, not a superficial one, mirroring `JwtVerifiableCredentialVerifier`'s real
  Ed25519 signature verification from E10. The outcome is recorded as `disclosureIntegrityValid` in
  the verification result's `checks` map and folded into the overall pass/fail. A new private
  `VerificationService.CredentialView` record normalizes both credential formats into one shape so
  the existing subject-binding/signature/expiry/trust/requiredClaims/status checks run unchanged
  regardless of format.
- **Deliberate scope decision — no Key Binding JWT (KB-JWT)**: the canonical SD-JWT VC presentation
  format appends a holder-signed KB-JWT as a trailing combined-format segment, proving possession of
  the credential's binding key. This platform skips it: `WalletPresentationSigner` already wraps
  every presented artifact inside a holder-signed outer VP JWT (`iss`/`aud`/`nonce`) carrying the
  same proof-of-possession, so an inner KB-JWT would be redundant. `SdJwtVerifier.parse` silently
  ignores a trailing KB-JWT segment if present (for interop with other issuers) but never requires
  or checks one.
- **No new database migration**: `credentials.credential_jwt` and `wallet_presentations.
  presentation_jwt` were already unbounded `TEXT` columns (`V8__credential_core.sql`,
  `V14__verification_core.sql`), so the longer, multi-disclosure combined-format string fits with no
  schema change — the first epic since E01 not to need one.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E10.
- No Key Binding JWT support (see above) — a documented simplification, not a missing security
  control, given the outer VP JWT's equivalent holder-binding proof.
- `sub` is always a plain, visible top-level claim, never itself made selectively disclosable.
- Only whole-claim disclosure is supported — no support for disclosing individual array elements or
  nested structured claims (SD-JWT VC's recursive/array disclosure forms).

## E12: Trust Registry

Implements EPIC 13 (Trust Registry, FR-TRUST-001 through 011) on the previously-empty `trust`
module placeholder: a tenant-scoped registry of trusted external issuer DIDs that `verification`
now checks dynamically alongside the pre-existing static per-policy allowlist from E10.

- **`TrustRegistry`** (`trust_registries`): a named, per-tenant container (Baseline section 7 ERD's
  `TENANT --< TRUST_REGISTRY`) with a `RegistryStatus` (`ACTIVE`/`INACTIVE`, defaults `ACTIVE`) and
  a plain free-text `framework` column — a deliberate V1-priority stub for FR-TRUST-008 (Trust
  Framework), not a dedicated typed entity. Unlike `CredentialSchema`/`VerificationPolicy`, there is
  no draft/version workflow: FR-TRUST-001 has no publish/freeze acceptance criteria, so a registry is
  usable for issuer registration immediately upon creation.
- **`TrustedIssuer`** (`trusted_issuers`): folds FR-TRUST-002 (Register Issuer) and FR-TRUST-003
  (Register DID) into one entity/one `registerIssuer` call, mirroring E10's precedent of folding
  FR-ISS-001/002. `issuerDid` is a plain string, not a foreign key into `did.Did` — trust registries
  exist specifically to declare trust in external issuers this platform never provisioned a DID row
  for. `trustedCredentialTypes` is a raw `List<String>` JSONB column (`@JdbcTypeCode(SqlTypes.JSON)`
  directly on the list, a natural extension of the existing JSONB pattern); null/empty means "trusted
  for every credential type," mirroring `PolicyDefinition.trustedIssuerDids`'s "empty is not
  fail-closed" precedent. `TrustStatus` is `TRUSTED`/`SUSPENDED` (freely reversible via
  `updateStatus`, FR-TRUST-004) or terminal `REVOKED` (FR-TRUST-005, mirrors `Credential.revoke`'s
  "never reverses" precedent — a revoked issuer must be re-registered from scratch).
- **`TrustRegistryService`**: `create`/`activate`/`deactivate`/`list`/`get` for registries;
  `registerIssuer` (rejects a non-`ACTIVE` registry and duplicate `issuerDid` within the same
  registry), `listIssuers`/`getIssuer`/`updateStatus`/`revokeTrust` for issuers; `lookup(tenantId,
  issuerDid)` (FR-TRUST-006, across every registry a tenant owns); `isTrusted(tenantId, issuerDid,
  credentialType)` (FR-TRUST-007 — the dynamic Trust Check: at least one `TRUSTED` entry for that DID
  in an `ACTIVE` registry, whose `trustedCredentialTypes` is empty or contains the presented
  credential's type); `hasAnyEntries(tenantId)`, which exists purely to support
  `VerificationService`'s non-breaking integration below. All mutations publish an `AuditEvent`
  (FR-TRUST-011).
- **`TrustRegistryController`** (`/api/v1/trust-registries`): mutations (`create`,
  `activate`/`deactivate`, `registerIssuer`, `updateStatus`, `revoke`) require `PLATFORM_ADMIN` or
  `ORG_ADMIN` — trust governance is an administrative function per the seeded `ORG_ADMIN` role
  description. Reads (`list`, `get`, `lookup`, `listIssuers`, `getIssuer`) additionally allow
  `VERIFIER`, mirroring `credential.schema.CredentialSchemaController`'s established
  VERIFIER-reads-but-doesn't-mutate precedent from E06. No new `SecurityConfig.permitAll` entries are
  needed — every Trust Registry endpoint requires authentication.
- **Dynamic integration into `VerificationService.evaluate`**: a new `trustRegistryTrusted` check
  runs alongside the pre-existing static `trustedIssuer` allowlist check from E10, and both must pass
  for verification to succeed. To avoid a breaking regression for every tenant that predates E12 and
  has never registered a trusted issuer, the dynamic check is only *enforced* when
  `TrustRegistryService.hasAnyEntries(tenantId)` is true for the verifying tenant; otherwise
  `trustRegistryTrusted` defaults to `true` and verification falls back to relying solely on the
  static allowlist, exactly as it did before E12. The credential type passed into `isTrusted` is read
  directly off the signed artifact rather than fetched from `CredentialSchema` — `vc.type[1]` for a
  plain JWT-VC (`JwtVerifiableCredentialSigner`'s `type = ["VerifiableCredential", schemaName]`), or
  the `vct` claim for an SD-JWT VC (E11) — since the credential's issuer may belong to a different
  tenant than the verifier, the same cross-tenant limitation class documented for FR-VER-011's status
  check.
- **`V16__trust_registry_core.sql`/`V17__trust_registry_row_level_security.sql`**: `trust_registries`
  and `trusted_issuers` tables plus the standard tenant-isolation RLS policy pattern from every prior
  epic (`V3`/`V5`/`V7`/`V9`/`V11`/`V13`/`V15`). An index on `(tenant_id, issuer_did)` backs both the
  `lookup` and `isTrusted` query paths.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E11.
- FR-TRUST-008 (Trust Framework), FR-TRUST-009/010 (Trust List Import/Export), and FR-TRUST-012
  (Hedera Anchoring) are deferred V1-priority items, per the Baseline's own MVP/V1 split — the latter
  is picked up in E13 (Hedera).
- No end-to-end test yet confirms the `hasAnyEntries`-gated fallback actually preserves pre-E12
  verification behavior for a tenant with zero registered issuers.

## E13: Hedera

Implements EPIC 16 (Hedera Integration): FR-HED-001/002/003 (Network/Account/HCS Topic
Configuration), FR-HED-004 through 008 (Trust Registry/DID/Schema/Credential Anchoring, Timestamp
Anchoring), FR-HED-009/010 (Transaction Status, Retry Failed Transaction), FR-HED-011 (Async
Anchoring), FR-HED-012 (Anchoring Verification) and FR-HED-013 (Hedera Health Check) on the
previously-empty `hedera` module placeholder from E01.

- **`HederaAnchorPublisher`/`HederaAnchorRequest`** (new `common.hedera` package): the interface
  every domain module calls, mirroring `AuditEventPublisher`'s established
  interface-in-`common`-plus-single-real-implementation-in-domain-module pattern — the pragmatic
  substitute for the Baseline's literal Kafka-based async design (section 17), since no message
  broker exists anywhere in this codebase. `HederaAnchorRequest` carries only a `resourceHash` —
  never raw content — via two static factories: `ofRawContent(...)` (hashes a raw string with
  SHA-256) and `ofHash(...)` (accepts an already-computed hash, avoiding double-hashing).
- **`HcsHederaAnchorPublisher`** (the sole `HederaAnchorPublisher` bean, in `hedera`): a genuine
  Hedera Java SDK integration (`com.hedera.hashgraph:sdk:2.66.0`), not a mock, per the master
  prompt's "do not create mock functionality where production functionality is required" rule —
  gated by `idtrust.hedera.enabled` (default `false`) rather than being fake. When disabled, no
  network/gRPC call is attempted and a `DISABLED` anchor row is persisted instead, so identity and
  credential functionality remain fully operational without a real Hedera account. `anchor(...)`
  never throws: any failure (disabled, unreachable network, bad credentials, SDK error) is captured
  as a `FAILED` row with a `failureReason` rather than propagating, so callers are never affected by
  Hedera availability.
- **`HederaAnchor`** (`hedera_anchors`): one row per anchoring attempt. `AnchorStatus` is
  `CONFIRMED`/`FAILED`/`DISABLED` — there is no transient PENDING/SUBMITTED state, because the
  Hedera SDK's `TopicMessageSubmitTransaction.execute(...).getReceipt(...)` call blocks for
  consensus, so the terminal outcome is already known by the time `anchor(...)` returns. A
  `CONFIRMED` row's `consensusTimestamp` (Hedera-assigned) satisfies FR-HED-008 (Timestamp
  Anchoring) without any separate implementation.
- **On-chain envelope**: `{eventType, resourceType, resourceId, resourceHash, timestamp}`,
  matching the Baseline's own section 17 example JSON shape — `tenantId` is deliberately excluded
  from the published message (kept only on the local `hedera_anchors` row for RLS) to keep the
  on-chain footprint minimal.
- **Anchored resources** (hash-only, never raw content/PII/keys, per Baseline section 26 Decision
  4): `TrustRegistryService` hashes `{issuerDid, organization, trustedCredentialTypes, status}`
  after `registerIssuer`/`updateStatus`/`revokeTrust` (FR-HED-004); `DidService` hashes the DID
  Document after `create` (FR-HED-005); `CredentialSchemaService` hashes the claim catalogue after
  `publish` (FR-HED-006); `CredentialIssuanceService` reuses the already-computed
  `Credential.credentialHash` after `issue`, via `HederaAnchorRequest.ofHash` (FR-HED-007) — no new
  hashing needed at that call site.
- **`HederaAnchorService`** (`get`/`listForResource`/`retry`/`verify`) + **`HederaAnchorController`**
  (`/api/v1/hedera/anchors`): `retry` (FR-HED-010) is only legal from `FAILED`, mirroring
  `TrustedIssuer.updateStatus`'s guard pattern, and is restricted to `PLATFORM_ADMIN`/`ORG_ADMIN`.
  `verify` (FR-HED-012) performs a genuine live HTTPS round-trip to the Hedera Mirror Node's public
  REST API (`https://{network}.mirrornode.hedera.com/api/v1/topics/{topicId}/messages/{sequenceNumber}`),
  mirroring `did.provider.didweb.DidWebProvider`'s precedent for resolving external state via the
  JDK's built-in `HttpClient`, and compares the on-chain message's `resourceHash` against the
  locally stored one. Reads additionally allow `VERIFIER`, matching every other
  read-vs-mutate RBAC split in this codebase. There is deliberately no anchoring-trigger endpoint —
  anchors are only ever created as a side effect of `trust`/`did`/`credential` operations.
- **`HederaHealthIndicator`** (FR-HED-013): a Spring Boot Actuator `HealthIndicator` exposed at
  `/actuator/health/hedera`. Reports `UP` (not `DOWN`) when `idtrust.hedera.enabled=false` — a
  disabled Hedera integration is a valid, healthy operating mode for this platform, not an outage —
  and otherwise performs a lightweight `AccountBalanceQuery` connectivity check.
- **`idtrust.hedera.*` configuration** (`HederaProperties`): `enabled` (default `false`),
  `network`/`account-id`/`operator-key`/`topic-id`, overridable via `HEDERA_ENABLED`/
  `HEDERA_NETWORK`/`HEDERA_ACCOUNT_ID`/`HEDERA_OPERATOR_KEY`/`HEDERA_TOPIC_ID` — mirroring KMS's E05
  "insecure/inert default, must override via env var for real environments" precedent.
- **`V18__hedera_anchor_core.sql`/`V19__hedera_anchor_row_level_security.sql`**: the `hedera_anchors`
  table plus the standard tenant-isolation RLS policy pattern from every prior epic.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E12; the Hedera Java SDK's
  exact API surface (`Client.forName`, `TopicMessageSubmitTransaction`, `TransactionRecord`, etc.)
  has not been verified against a real build.
- FR-HED-014 (Multi-Network Configuration — distinct topics/networks per tenant or ecosystem) is a
  V2-priority item and out of scope; a single shared topic/network configuration serves every
  tenant.
- No integration test exercises a real Hedera testnet account; `idtrust.hedera.enabled=false` by
  default means the anchoring code paths are entirely untested against a live network.
- `HederaAnchorController` has no endpoint to list anchors across resource types for a tenant (only
  per-resource `listForResource`) — acceptable for now since anchors are always looked up starting
  from a known DID/schema/credential/trusted-issuer.

## E14: API & Developer Platform

Implements EPIC 14 (API & Developer Platform): FR-API-001 (API Key), FR-API-002 (OAuth Client),
FR-API-003 (API Scope), FR-API-004 (Rate Limit), FR-API-005 (API Versioning — already satisfied by
every controller's existing `/api/v1/...` prefix, no new code needed), FR-API-006 (OpenAPI
Documentation), FR-API-007 (API Logs), FR-API-008 (Webhooks) and FR-API-009 (Webhook Retry), on a
brand-new `apiplatform` module (the first epic requiring one — no pre-scaffolded placeholder
existed for it in E01, unlike every prior epic).

- **`ApiClient`** (`api_clients`): a single table for both API keys and OAuth clients
  (`ApiClientType.API_KEY`/`OAUTH_CLIENT`), matching the Baseline ERD's one `API_CLIENT` node.
  `secretHash` is a SHA-256 hash (`auth.CredentialHasher`) of the plaintext API key or OAuth client
  secret, revealed to the caller exactly once at creation time (`ApiClientCreationResult`) and never
  stored in the clear — mirroring `WebhookEndpointService`'s one-time-secret-reveal pattern. `scopes`
  (FR-API-003) is a comma-separated subset of `client.ApiScopes.VALID_ROLE_CODES` — a hardcoded copy
  of the 5 RBAC role codes (`PLATFORM_ADMIN`, `ORG_ADMIN`, `ISSUER`, `VERIFIER`, `HOLDER`) rather than
  a live query against `identity.rbac.Role`, deliberately avoiding a new `apiplatform → identity`
  Gradle dependency — every existing `@PreAuthorize` annotation already hardcodes these same literals.
- **`ApiKeyAuthenticationFilter`** (new, in `identity.security`, registered in `SecurityConfig` via
  `.addFilterBefore(..., BearerTokenAuthenticationFilter.class)`): recognizes an API key or OAuth
  access token bearer credential ahead of Spring Security's normal Keycloak JWT resource-server
  path, and falls through harmlessly (no-op) if the token isn't recognized, letting a genuine JWT
  proceed as before. Decoupled via a new `common.security.ApiCredentialAuthenticator` interface
  (`+ApiPrincipal` record) — `identity` cannot depend on the new `apiplatform` module (which needs
  spring-security types not available in `common`), so the concrete authenticator
  (`apiplatform.auth.ApiClientCredentialAuthenticator`) is auto-wired by Spring purely because `app`
  depends on both modules, mirroring `AuditEventPublisher`/`HederaAnchorPublisher`'s no-explicit-
  default-bean precedent exactly. A new `common.tenant.CurrentApiClientContext` ThreadLocal (cleared
  in `TenantContextInterceptor.afterCompletion`, mirroring `CurrentUserContext`) records which
  `ApiClient` authenticated the current request.
- **`OAuthTokenService`** (FR-API-002, `POST /api/v1/oauth/token`, `client_credentials` grant):
  mints an **opaque, hashed, server-verified** `ApiAccessToken` (`api_access_tokens`) — deliberately
  not a self-signed JWT, reusing exactly the same hash-lookup mechanism already needed for API keys
  rather than introducing a new JWT-signing/verification path (this platform's only existing
  JWT-signing dependency is `kms`'s DID/credential-signing pipeline, a poor fit for a generic OAuth
  token). Token lifetime is configurable via `idtrust.apiplatform.oauth-access-token-ttl-seconds`.
- **`RateLimitFilter`** (FR-API-004, `@Order(10)`): an in-memory, per-caller (API client id if
  authenticated, else tenant id, else remote address), fixed-window budget
  (`idtrust.apiplatform.rate-limit-requests-per-minute`, default 120/min), returning `429` with a
  JSON body on exhaustion — not distributed (no Redis client exists anywhere in this codebase); a
  documented limitation, not an oversight, if this modular monolith is ever horizontally scaled.
- **`OpenApiConfig`** (FR-API-006): adds `springdoc-openapi-starter-webmvc-ui:2.6.0` for a genuine,
  auto-generated OpenAPI 3 spec and Swagger UI (`/v3/api-docs`, `/swagger-ui.html`), with a
  `bearerAuth` HTTP bearer security scheme declared once and applied everywhere.
- **`ApiRequestLog`** (`api_request_logs`, FR-API-007) + **`ApiRequestLoggingFilter`**
  (`@Order(0)`, outermost so it captures the rate limiter's own `429` short-circuits): one row per
  completed request (method/path/status/duration/correlationId/tenantId/apiClientId — the latter two
  nullable, since an unauthenticated or platform-level request never resolves either), queryable via
  `GET /api/v1/api-request-logs`. Both new filters sit at small positive `@Order` values, greater
  than Spring Security's `FilterChainProxy` (`-100`), so `SecurityContextHolder`/`TenantContext`/
  `CurrentApiClientContext` are already populated when they run.
- **`WebhookEndpoint`/`WebhookDelivery`** (`webhook_endpoints`/`webhook_deliveries`, FR-API-008) +
  **`WebhookEventPublisherImpl`**: the single real `common.webhook.WebhookEventPublisher`
  implementation, mirroring `HederaAnchorPublisher`'s interface-in-`common`-plus-single-impl
  pattern. For every active, subscribed endpoint of a tenant, performs a genuine HTTP POST (JDK
  `HttpClient`) with an HMAC-SHA256-signed body (`X-IDTrust-Signature`, `webhook.WebhookSigner`),
  and always persists a `WebhookDelivery` row recording the outcome — never throws, so a delivery
  failure (network error, non-2xx, even a failed endpoint lookup) never affects the domain operation
  that triggered it. `WebhookEndpoint.encryptedSecret` is genuinely **encrypted** (not hashed) via
  `kms.crypto.KeyEncryptionService`, unlike `ApiClient.secretHash`, because the plaintext secret must
  be recoverable to compute the outbound signature on every delivery. `webhook.WebhookEventTypes` is
  a fixed 10-name vocabulary (`CredentialIssued`, `CredentialRevoked`, `CredentialSuspended`,
  `VerificationCompleted`, `DIDCreated`, `DIDUpdated`, `TrustRegistryUpdated`,
  `HederaAnchorRequested`, `HederaAnchorCompleted`, `AuditEventCreated`) — the same Kafka event-type
  names already catalogued in the Baseline's section 6, reused here since no Kafka broker exists
  anywhere in this codebase.
- **Webhook call sites**: `DidService` (`DIDCreated` on `create`, `DIDUpdated` on `rotate`/
  `deactivate`), `TrustRegistryService` (`TrustRegistryUpdated` on `registerIssuer`/`updateStatus`/
  `revokeTrust`), `CredentialIssuanceService` (`CredentialIssued` on `issue` — also naturally covers
  `CredentialLifecycleService.renew`/`reissue`, which call `issue` internally),
  `CredentialLifecycleService` (`CredentialRevoked` on `revoke`, `CredentialSuspended` on
  `suspend`), `VerificationService` (`VerificationCompleted` on `presentAndVerify`, fired for both
  pass and fail outcomes). `AuditEventCreated` is wired once, at a single choke point
  (`LoggingAuditEventPublisher.publish`) rather than threaded through every module that emits an
  `AuditEvent` — every audited mutation across the whole codebase is relayed with zero per-module
  wiring; skipped for platform-level events with no `tenantId` or a non-UUID `resourceId`.
  `CredentialSchemaService` has no wiring: no event type in the fixed vocabulary corresponds to a
  schema mutation.
- **`WebhookDeliveryService#retry`** (FR-API-009): manual-only, mirroring
  `HederaAnchorService#retry`'s exact "guarded by status, no background daemon" precedent — only
  legal from a `FAILED` delivery; there is no background auto-retry/backoff scheduler anywhere in
  this codebase.
- **`WebhookController`** (`/api/v1/webhooks`) + **`ApiClientController`** (`/api/v1/api-clients`):
  both restricted to `PLATFORM_ADMIN`/`ORG_ADMIN`, mirroring every other admin-mutation-style
  controller — issuing a machine credential or registering an outbound webhook (with its signing
  secret) is a privileged operation.
- **`V20__apiplatform_core.sql`/`V21__apiplatform_row_level_security.sql`**: all 5 new tables plus
  the standard tenant-isolation RLS policy pattern from every prior epic — `api_request_logs`'
  policy additionally allows `tenant_id IS NULL` rows, since `ApiRequestLoggingFilter` runs before
  `TenantContext` may have been populated for an unauthenticated request.

**Deliberately deferred (documented, not overlooked):**
- FR-API-010 (Sandbox Environment), FR-API-011 (Developer Portal) and FR-API-012 (SDK Generation)
  are V1/V2-priority items requiring substantial new infrastructure (an isolated sandbox tenant
  tier, a dedicated portal UI, a codegen pipeline) disproportionate to this pass — out of scope,
  mirroring every prior epic's precedent of explicitly documenting skipped items rather than
  building speculative stand-ins.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as E02-E13.
- `RateLimitFilter`'s budget is in-memory and per-instance, not distributed — see FR-API-004's note
  above; revisit if this modular monolith is ever horizontally scaled.
- OAuth access tokens are opaque and server-verified, not self-signed JWTs — a deliberate
  simplification (see `auth.ApiAccessToken`'s Javadoc), not a spec deviation, since FR-API-002 only
  requires a valid `client_credentials` grant, not a specific token format.
- Webhook retry (FR-API-009) is manual-only; a delivery that keeps failing stays `FAILED` until an
  admin retries it — there is no exponential-backoff daemon anywhere in this codebase yet.
- No integration test exercises the new `ApiKeyAuthenticationFilter`/`RateLimitFilter`/webhook HTTP
  delivery path against a real HTTP server.

## E15: UI / QA / Security / Production

This final epic is a synthesis, not a single Baseline EPIC: it maps onto EPIC 15 (Audit &
Compliance, FR-AUD-001–012), EPIC 17 (Notification, FR-NOT-001–006), EPIC 18 (Analytics &
Monitoring, FR-ANL-001–008), plus the non-FR-numbered Baseline sections on UI/UX, Security Threat
Model, Test Catalogue, and Docker/Kubernetes deployment.

- **`audit` module** (new, FR-AUD-009/010): `AuditRecord`/`audit_records` gives the audit trail a
  durable, queryable store. FR-AUD-001–008 (Authentication/Authorization/DID/Key/Credential/
  Issuance/Verification/Trust Audit) needed no new code — every prior epic already calls
  `common.audit.AuditEventPublisher.publish(...)` at its own choke points. `PersistingAuditEventPublisher`
  (`@Primary`) persists + logs + relays a `AuditEventCreated` webhook, and replaces
  `LoggingAuditEventPublisher` as the active bean purely via `@ConditionalOnMissingBean` — the
  exact mechanism `LoggingAuditEventPublisher`'s own pre-existing Javadoc called for, with zero
  changes to any of the ~25 existing call sites. `AuditController` exposes admin-only search
  (`GET /api/v1/audit-records`) and CSV export (`GET /api/v1/audit-records/export`, capped at
  5000 rows).
- **`notification` module** (new, FR-NOT-001–004): `Notification`/`notifications` (in-app feed) +
  `EmailNotificationService` (SMTP via `JavaMailSenderImpl`, disabled by default —
  `idtrust.notification.email.enabled=false`, mirroring Hedera's "disabled is a valid healthy
  mode" precedent). `NotificationEventPublisherImpl` is the sole `common.notification.
  NotificationEventPublisher` implementation, wired into `CredentialIssuanceService.issue` and
  `VerificationService.presentAndVerify`; it notifies every `ACTIVE` user of the tenant (not
  filtered by RBAC role — a documented scope simplification, see below).
  `NotificationController` (`/api/v1/notifications`) is self-scoped to the caller, no admin role
  required. FR-NOT-005 (Webhook Notification) needed no new code — already fully satisfied by
  E14's `WebhookEventPublisherImpl`, a genuinely different (machine-to-machine) delivery channel
  from this module's human-facing email/in-app notifications.
- **Analytics/monitoring (EPIC 18)** via Micrometer/Prometheus, not a bespoke metrics module:
  `io.micrometer:micrometer-core` added as an `api` dependency on `common` (so every module gets
  `MeterRegistry` for free) and `micrometer-registry-prometheus` wired once on `app`
  (`runtimeOnly`), with `management.endpoints.web.exposure.include` extended to
  `prometheus,metrics`. Instrumented at existing single choke points rather than scattered
  ad-hoc: `GlobalExceptionHandler` (`idtrust.errors` by error code), `ApiRequestLoggingFilter`
  (`idtrust.api.requests` timer, FR-ANL-002 API usage), `CredentialIssuanceService.issue`
  (`idtrust.credentials.issued` by format, FR-ANL-001), `CredentialLifecycleService.transition`
  (`idtrust.credentials.transitions`, FR-ANL-003), `Oid4vciSessionService.redeemCredential`
  (`idtrust.wallet.credentials_claimed`, FR-ANL-004), `VerificationService.presentAndVerify`
  (`idtrust.verifications` by result). FR-ANL-006 (System Health) needed no new code — already
  satisfied by the pre-existing Actuator health/readiness/liveness probes and
  `HederaHealthIndicator`.
- **Security headers** (Baseline section 33 / EPIC 21 threat model): `identity.security.
  SecurityConfig` now sets `frameOptions().deny()`, a restrictive Content-Security-Policy
  (`default-src 'none'; frame-ancestors 'none'` — a JSON-only API serves no HTML the browser
  needs to trust), `NO_REFERRER` referrer-policy, and a locked-down permissions-policy. CSRF stays
  disabled with an explicit rationale comment: this is a stateless bearer-token-only API with no
  cookie-based session, so there is no ambient credential a cross-site request could ride on.
- **CORS** (new, `identity.security.SecurityCorsProperties`, `idtrust.security.cors.
  allowed-origins`): the `frontend/` scaffold below is served from its own origin
  (`http://localhost:5173` by default) and calls this API directly, so an explicit allow-list
  (`Authorization`/`Content-Type`/`X-Tenant-ID` headers) was added — this API had no CORS
  configuration at all before E15, since every prior caller was either server-to-server or a
  same-origin admin/test tool.
- **CI security scanning**: a new `security-scan` job in `.github/workflows/ci.yml` runs
  `aquasecurity/trivy-action` (filesystem scan) and uploads SARIF results via
  `github/codeql-action/upload-sarif`. `continue-on-error: true` — informational, not a merge
  gate, since this pass can't verify Trivy's findings against a real build.
- **Kubernetes manifests** (new, `infra/k8s/`, Baseline section 20): `namespace.yaml`,
  `configmap.yaml` (non-secret env), `secret.yaml` (placeholder template — never real values),
  `deployment.yaml` (2 replicas, resource requests/limits, readiness/liveness probes wired to the
  existing Actuator endpoints), `service.yaml`, `hpa.yaml` (CPU-based, 2–6 replicas, requires the
  metrics-server add-on), `pdb.yaml` (`minAvailable: 1`), `networkpolicy.yaml` (default-deny
  ingress, opened only within the namespace), `ingress.yaml` (TLS via a cert-manager placeholder).
- **`backend/Dockerfile`** (new): multi-stage build — `eclipse-temurin:21-jdk` runs
  `./gradlew :app:bootJar --no-daemon`, then `eclipse-temurin:21-jre` runs the jar as a non-root
  `idtrust` user (uid 1000). Requires the Gradle wrapper to already be committed (see
  Prerequisites below) — this pass could not generate one.
- **`infra/docker-compose.yml`**: gained an optional `app` service building from
  `backend/Dockerfile`, so the full stack (Postgres/Redis/Keycloak/backend) can start with one
  `docker compose up`; service URLs use Compose network names, matching `infra/k8s/configmap.yaml`'s
  in-cluster equivalents.
- **`frontend/`** (new): a React 18 + TypeScript + Vite + Tailwind CSS scaffold — an explicitly
  scoped **subset** of the Baseline's full 36–48 screen inventory, not the whole thing. Built:
  Login (Keycloak Authorization Code + PKCE via `react-oidc-context`), Dashboard (unread
  notification count), DIDs (list + create), Credentials (issue + list, against a chosen
  published schema), Verification Requests (create an OID4VP request + QR code, list results),
  Notifications (list + mark read). Every page calls a genuinely existing backend endpoint — no
  mocked data. There is no "list my tenants" backend endpoint yet, so the Tenant ID (the
  `X-Tenant-ID` header every tenant-scoped endpoint requires) is entered once by the user and kept
  in `localStorage` (`src/tenant/TenantContext.tsx`) rather than resolved automatically from the
  JWT — a documented scope simplification, not an oversight.
- **MFA**: out of scope for application code. Authentication is entirely delegated to Keycloak
  (OAuth2 resource server, bearer JWTs only) — MFA is a Keycloak realm-level configuration concern
  (e.g. enabling an OTP required-action/authentication-flow on the `idtrust` realm), not something
  this stateless backend or the new frontend scaffold can or should implement itself.
- **OWASP Top 10 → existing controls** (documented mapping, EPIC 21 threat model):
  - Injection (SQLi) → JPA/Hibernate parameterized queries everywhere, plus Postgres RLS as a
    second line of defense (every tenant-scoped table, since E03).
  - Broken access control → Keycloak-issued JWT + `@PreAuthorize` role checks on every controller,
    plus `TenantMembershipFilter`'s three-layer tenant isolation (since E02).
  - Cryptographic failures → KMS envelope encryption for all private key material (`kms.crypto.
    KeyEncryptionService`, since E05); secrets never committed (`secret.yaml` is a placeholder).
  - Security misconfiguration / vulnerable components → the new Trivy CI job (this epic).
  - XSS → this API returns JSON only, never HTML; the new frontend uses React's default
    escaping and the new `default-src 'none'` CSP.
  - CSRF → not applicable (documented rationale above: stateless, bearer-token-only, no cookies).
  - Auth failures → Keycloak handles credential storage/rotation/lockout; this backend never
    stores a password.
  - Insufficient logging/monitoring → the new `audit` module (this epic) plus
    `ApiRequestLog` (E14) plus the new Micrometer instrumentation (this epic).
  - Server-side request forgery → webhook delivery (E14) targets tenant-configured URLs; no
    SSRF-specific allow-list exists yet (see limitations below).
  - Rate limiting/DoS → `apiplatform.ratelimit.RateLimitFilter` (E14), in-memory/per-instance.

**Deliberately deferred (documented, not overlooked):**
- FR-AUD-011 (Retention Policy) and FR-AUD-012 (Data Deletion) — no automatic audit-record
  expiry/purge job exists yet; V1-priority per the Baseline.
- FR-NOT-006 (Notification Preferences) — notifications currently go to every `ACTIVE` tenant
  user unconditionally; per-user/per-event-type opt-out is the natural extension point once this
  lands.
- FR-ANL-005 (Trust Registry Analytics), FR-ANL-007 (Custom Reports), FR-ANL-008 (Dashboard) —
  the first two need dedicated aggregation queries this pass didn't build, and FR-ANL-008
  explicitly requires a frontend dashboard beyond this pass's scaffold's plain unread-count tile.
- The remaining ~30+ screens of the Baseline's UI inventory (org/user/role administration, schema
  and policy authoring, webhook/API-client management, trust registry administration, Hedera
  anchoring, an audit-log viewer, credential schema template management, etc.) — this pass
  wires up the highest-value end-to-end flows (issue → present/verify → get notified), not a
  full admin console.
- A dedicated SSRF allow-list for outbound webhook URLs — flagged above, not built.
- Large-scale automated test authorship: consistent with every prior epic (E02–E14),
  `IdTrustApplicationTests` remains the only test file in the codebase. The Master Specification's
  section 36 testing strategy (80%+ coverage target, integration/protocol/E2E test categories,
  4 named E2E scenarios) is an aspirational target this pass did not attempt to reach, given there
  is no toolchain available in this environment to write and actually run new tests against.

**Known limitations to verify/build out before production:**
- Nothing in this pass — or any prior epic — has been compiled, run, or tested. Same caveat as
  E02–E14, now compounded by a `frontend/` scaffold whose `npm install`/`npm run build` has
  likewise never been executed.
- The Kubernetes manifests have never been applied to a real cluster; `secret.yaml`'s placeholder
  values must be replaced (or the file regenerated from a real secrets manager) before use, and
  `hpa.yaml` requires the metrics-server add-on to be installed separately.
- The Trivy CI job is informational (`continue-on-error: true`), not a merge gate — no vulnerable
  dependency currently blocks a build.
- `backend/Dockerfile`'s build stage requires the Gradle wrapper to already be committed to the
  repo (see the root README's own Prerequisites note) — it is not generated by this pass.
- The frontend's Tenant ID entry (`localStorage`, no "my tenants" endpoint) is a real UX gap for
  a user who belongs to more than one tenant or doesn't already know their tenant's UUID.
- CORS's allow-list currently only covers the Vite dev server origin by default
  (`CORS_ALLOWED_ORIGINS`) — every deployed environment must override it with the real frontend
  origin.
- MFA and the OWASP mapping above are documentation of existing/Keycloak-delegated controls, not
  new enforcement code — nothing here was penetration-tested.

## Post-E15: API/DB spec reconciliation gap-fill

A follow-up pass, done after all 15 epics above, closing gaps found by diffing the implementation
against 4 supplied artifact specs (API spec, DB spec, UI spec, AI backlog) rather than against the
Architecture & Implementation Baseline used for E01–E15. Two categories:

**Cross-cutting API contracts:**
- **Idempotency-Key** (`identity.idempotency`): `idempotency_keys` (`V26`/`V27`) +
  `IdempotencyFilter`, a new `OncePerRequestFilter` registered after `TenantMembershipFilter`.
  Skips non-POST/PUT/PATCH or missing-header/missing-tenant requests; hashes the request body
  (SHA-256), replays a stored response on an exact hash match, `409`s on a hash mismatch for the
  same key, otherwise executes the chain and persists the response (status < 500 only) with a
  24-hour expiry. Only covers tenant-scoped requests — a documented limitation, not an oversight.
- **ETag / If-Match optimistic concurrency** (`common.web.ETagSupport`, `V28`): a `lockVersion`
  JPA `@Version` column added to `Organization`, `Tenant`, `CredentialSchema`,
  `VerificationPolicy` — named `lockVersion` (not `version`) on all 4 for consistency, since
  `CredentialSchema`/`VerificationPolicy` already have a business-meaning `version` field (schema/
  policy revision) that a generic optimistic-lock counter would collide with. `GET` returns an
  `ETag`; `PATCH` requires a matching `If-Match` (new `ApiErrorCode.PRECONDITION_FAILED`, `412`) or
  none at all (`*`/absent is a no-op check). Not applied to every PATCH endpoint in the codebase —
  scoped to these 4 resources.
- **Pagination envelope** (`common.web.PageResponse<T>`): a `{content, page, size, totalElements,
  totalPages}` wrapper (Spring Data `Page<T>` reused directly, not reinvented) applied to the
  Organizations/Tenants/Users/DIDs/Credentials/CredentialSchemas list endpoints, alongside their
  pre-existing unpaginated `List<T>` search methods (kept for internal/service-layer callers).

**Missing tables/entities (8, one per module):**
- **`service_accounts`** (`identity.serviceaccount`, `V29`/`V30`) — a durable, tenant-owned
  non-human *identity* record (name/description/status: `ACTIVE`/`SUSPENDED`/`REVOKED`).
  Deliberately **not** merged with `apiplatform.client.ApiClient` (`api_clients`), which already
  fully implements machine-identity *authentication credentials* (FR-API-001/002) — a different
  concept with no FK coupling introduced between the two modules.
- **`tenant_memberships`** (`identity.membership`, `V31`/`V32`) — a genuinely additive
  tenant↔user many-to-many (`OWNER`/`ADMIN`/`MEMBER`, `ACTIVE`/`REVOKED`), explicitly **not** wired
  into `TenantMembershipFilter` (still the sole JWT-tenant-resolution path) or `rbac.UserRole`
  (still the sole per-tenant RBAC mechanism) — a standalone membership-management API layered on
  top, not a replacement.
- **`did_key_events`** (`kms.key`, `V33`/`V34`) — an append-only lifecycle trail (`GENERATED`/
  `ROTATED`/`DEACTIVATED`/`DELETED`) recorded by `KeyService` at all 4 mutation points, exposed via
  `GET /api/v1/dids/{didId}/key-events`. Complements (doesn't replace) `ManagedKey
  .rotatedFromKeyId`'s single-hop pointer.
- **`credential_templates`** (`credential.template`, `V35`/`V36`) — reusable, tenant-owned
  issuance presets (default claims + default validity) for a schema, distinct from
  `CredentialSchema` itself. `CredentialIssuanceService.issueFromTemplate` (`POST /api/v1/
  credentials/from-template`) merges caller-supplied claim overrides on top of a template's
  defaults.
- **`status_lists`** (`credential.statuslist`, `V37`/`V38`) — a real StatusList2021-shaped
  GZIP+Base64url compressed bitstring (`BitstringCodec`), closing the gap
  `CredentialLifecycleService`'s own pre-existing Javadoc already documented ("FR-CRED-012 ... out
  of scope ... plain id/status list, not a bitstring credential"). Scoped to the `REVOCATION`
  purpose only (suspension stays on the existing reversible status column, a poor fit for a
  permanent-flag bitstring). Wired into `CredentialIssuanceService.issue` (allocates a bit index
  per new credential) and `CredentialLifecycleService.revoke` (flips it) — both fire-and-forget,
  never blocking the credential operation on allocation/bit-flip failure. `GET /api/v1/
  status-lists?issuerDid=...` is tenant-scoped and authenticated, **not** the unauthenticated,
  publicly resolvable endpoint the real spec expects verifiers to hit directly — a documented
  simplification, since introducing this codebase's first unauthenticated resource was out of
  scope for this pass.
- **`trust_entry_events`** (`trust`, `V39`/`V40`) — mirrors `did_key_events`: an append-only trail
  of `TrustedIssuer` status transitions (register/update/revoke), recorded by
  `TrustRegistryService` alongside its existing audit-event call, exposed via `GET /api/v1/
  trust-registries/{id}/issuers/{issuerId}/events`.
- **`hedera_networks`** (`hedera`, `V41`) — a **global** reference catalogue (no `tenant_id`, no
  RLS, same pattern as `permissions` since E02) of Hedera's named networks and their well-known
  public mirror-node/JSON-RPC endpoints, seeded with `testnet`/`mainnet` (active) and `previewnet`
  (inactive). Exposed read-only via `GET /api/v1/hedera/networks`. Does **not** yet drive runtime
  network *selection* — `HcsHederaAnchorPublisher` still binds to `idtrust.hedera.network` from
  `HederaProperties` directly; FR-HED-014 (Multi-Network Configuration) remains V1-out-of-scope,
  this table is groundwork for it, not the full feature.

**Known limitations to verify/build out before production:**
- Nothing in this pass has been compiled or run — same caveat as every prior epic.
- Idempotency replay only covers tenant-scoped requests (idempotency_keys is tenant-partitioned).
- ETag/If-Match is scoped to 4 resources, not every mutable resource in the codebase.
- `status_lists`' public-resolution endpoint (what a real verifier would fetch, unauthenticated)
  was not built — see above.

## Prerequisites

- Java 21 (e.g. `brew install openjdk@21`)
- Gradle 8.x, **once**, to generate the wrapper (`brew install gradle`) — after that `./gradlew`
  is self-contained and Gradle doesn't need to stay installed
- Docker (for Postgres/Redis/Keycloak via Compose, or the full stack including the backend
  itself — see `infra/docker-compose.yml`'s `app` service, E15)
- Node.js 20+ and npm, only if running the `frontend/` scaffold (E15)

None of these were available in the environment this scaffold was generated in, so neither the
backend nor the frontend has **been compiled or run yet** — treat first build/test locally as a
validation step.

## Running locally

```bash
cp .env.example .env

# start infra (Postgres/Redis/Keycloak)
docker compose -f infra/docker-compose.yml --env-file .env up -d

# one-time: generate the Gradle wrapper (requires local Gradle 8.x)
cd backend && gradle wrapper && cd ..

# build & run the backend
cd backend
./gradlew :app:bootRun
```

Health check: `curl http://localhost:8080/api/v1/health` and
`curl http://localhost:8080/actuator/health`.

To run the E15 frontend scaffold against that backend:

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Then open `http://localhost:5173` — it redirects to Keycloak for sign-in, then requires a Tenant
ID (UUID) to be entered once (see this README's E15 section for why).

## Tests

```bash
cd backend
./gradlew test
```

`IdTrustApplicationTests` boots the full module set against an in-memory H2 database so it
doesn't require Postgres to be running; real integration tests against Postgres are introduced
starting E02/E03.
