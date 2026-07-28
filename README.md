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
| E02 | IAM / Tenancy / RBAC | ⚠️ implemented, unverified (no local build yet) |
| E03 | Database / Row-Level Security | ⚠️ implemented, unverified (no local build yet) |
| E04 | DID | ⚠️ implemented, unverified (no local build yet) |
| E05 | KMS / Key Lifecycle | ⚠️ implemented, unverified (no local build yet) |
| E06 | Schemas / Templates | ⚠️ implemented, unverified (no local build yet) |
| E07 | Credential Issuance | ⚠️ implemented, unverified (no local build yet) |
| E08 | Status / Revocation | ⚠️ implemented, unverified (no local build yet) |
| E09 | OID4VCI / Wallets | ⚠️ implemented, unverified (no local build yet) |
| E10 | OID4VP / Verification | pending |
| E11 | SD-JWT | pending |
| E12 | Trust Registry | pending |
| E13 | Hedera | pending |
| E14 | Enterprise Integrations | pending |
| E15 | UI / QA / Security / Production | pending |

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

## Prerequisites

- Java 21 (e.g. `brew install openjdk@21`)
- Gradle 8.x, **once**, to generate the wrapper (`brew install gradle`) — after that `./gradlew`
  is self-contained and Gradle doesn't need to stay installed
- Docker (for Postgres/Redis/Keycloak via Compose)

None of these were available in the environment this scaffold was generated in, so the backend
has **not been compiled or run yet** — treat first build/test locally as a validation step.

## Running locally

```bash
cp .env.example .env

# start infra
docker compose -f infra/docker-compose.yml --env-file .env up -d

# one-time: generate the Gradle wrapper (requires local Gradle 8.x)
cd backend && gradle wrapper && cd ..

# build & run
cd backend
./gradlew :app:bootRun
```

Health check: `curl http://localhost:8080/api/v1/health` and
`curl http://localhost:8080/actuator/health`.

## Tests

```bash
cd backend
./gradlew test
```

`IdTrustApplicationTests` boots the full module set against an in-memory H2 database so it
doesn't require Postgres to be running; real integration tests against Postgres are introduced
starting E02/E03.
