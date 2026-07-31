-- Bug fix: KeyService.delete() records a DELETED did_key_events row for the key being deleted,
-- then hard-deletes the key row itself in the same transaction. did_key_events.key_id was
-- NOT NULL REFERENCES keys(id) with no ON DELETE action, so deleting a key that has ANY
-- recorded event (every key does - generate() always records a GENERATED event) violates the FK
-- constraint and the delete always fails with a 500. did_key_events is an append-only history
-- table (see V33), so once a key is gone its past events should still be queryable by DID - only
-- the now-dangling key_id reference needs to go, not the row.
DO $$
DECLARE
    constraint_name text;
BEGIN
    SELECT tc.constraint_name INTO constraint_name
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu
      ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
    WHERE tc.table_name = 'did_key_events'
      AND tc.constraint_type = 'FOREIGN KEY'
      AND kcu.column_name = 'key_id';

    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE did_key_events DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;

ALTER TABLE did_key_events ALTER COLUMN key_id DROP NOT NULL;
ALTER TABLE did_key_events
    ADD CONSTRAINT did_key_events_key_id_fkey
    FOREIGN KEY (key_id) REFERENCES keys(id) ON DELETE SET NULL;
