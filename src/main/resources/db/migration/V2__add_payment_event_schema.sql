ALTER TABLE holds
    ADD COLUMN payment_id VARCHAR(100);

UPDATE holds
SET payment_id = 'legacy-' || id::text
WHERE payment_id IS NULL;

ALTER TABLE holds
    ALTER COLUMN payment_id SET NOT NULL;

ALTER TABLE holds
    ADD CONSTRAINT holds_payment_id_unique
        UNIQUE (payment_id);

CREATE TABLE payment_events (
                                event_id VARCHAR(100) PRIMARY KEY,
                                payment_id VARCHAR(100) NOT NULL,
                                hold_id UUID NOT NULL REFERENCES holds(id),
                                status VARCHAR(20) NOT NULL,
                                occurred_at TIMESTAMPTZ NOT NULL,
                                received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                CONSTRAINT payment_events_status_check
                                    CHECK (status IN ('approved', 'cancelled'))
);