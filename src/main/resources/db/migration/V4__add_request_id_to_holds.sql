ALTER TABLE holds
    ADD COLUMN request_id VARCHAR(100);

UPDATE holds
SET request_id = 'legacy-' || id::text
WHERE request_id IS NULL;

ALTER TABLE holds
    ALTER COLUMN request_id SET NOT NULL;

ALTER TABLE holds
    ADD CONSTRAINT holds_request_id_unique
        UNIQUE (request_id);