-- IDTrust baseline migration (E01 - Foundation).
--
-- Domain tables (organizations, tenants, users, dids, credentials, ...) are intentionally NOT
-- created here. They land in their owning epics:
--   E02/E03 - organizations, tenants, users, roles, permissions (+ Row Level Security policies)
--   E04     - dids
--   E05     - keys
--   E06-E08 - credential_schemas, credentials, credential_status
--   E09     - wallets, wallet_credentials
--   E10     - presentation_requests, presentations, verification_results, verification_policies
--   E12     - trust_registries, trusted_issuers
--
-- This migration only establishes the extensions every later migration will rely on.

CREATE EXTENSION IF NOT EXISTS pgcrypto;
