-- Backs the Idempotency-Key/If-Match cross-cutting API contract (Hardened API Specification):
-- adds a JPA @Version column to the entities whose PATCH endpoints support optimistic
-- concurrency via ETag/If-Match. Named lock_version (not version) because credential_schemas and
-- verification_policies already have a business-meaning "version" column (schema/policy revision
-- number), which must not be confused with or overwritten by Hibernate's optimistic-lock counter.
ALTER TABLE organizations ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE tenants ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE credential_schemas ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE verification_policies ADD COLUMN lock_version BIGINT NOT NULL DEFAULT 0;
