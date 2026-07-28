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
| E06 | Schemas / Templates | pending |
| E07 | Credential Issuance | pending |
| E08 | Status / Revocation | pending |
| E09 | OID4VCI / Wallets | pending |
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
