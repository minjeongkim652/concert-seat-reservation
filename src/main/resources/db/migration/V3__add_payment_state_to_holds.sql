ALTER TABLE holds
    ADD COLUMN payment_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

ALTER TABLE holds
    ADD COLUMN payment_occurred_at TIMESTAMPTZ;

ALTER TABLE holds
    ADD CONSTRAINT holds_payment_status_check
        CHECK (payment_status IN ('PENDING', 'APPROVED', 'CANCELLED'));