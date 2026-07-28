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
| E02 | IAM / Tenancy / RBAC | pending |
| E03 | Database / Row-Level Security | pending |
| E04 | DID | pending |
| E05 | KMS / Key Lifecycle | pending |
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
