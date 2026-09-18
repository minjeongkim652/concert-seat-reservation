CREATE TABLE performances (
                              id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                              name VARCHAR(200) NOT NULL
);

CREATE TABLE holds (
                       id UUID PRIMARY KEY,
                       performance_id BIGINT NOT NULL REFERENCES performances(id),
                       user_id VARCHAR(100) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       expires_at TIMESTAMPTZ NOT NULL,
                       created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT holds_status_check
                           CHECK (status IN ('ACTIVE', 'CANCELLED', 'EXPIRED', 'PAID'))
);

CREATE TABLE seats (
                       id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       performance_id BIGINT NOT NULL REFERENCES performances(id),
                       label VARCHAR(20) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       hold_id UUID REFERENCES holds(id),

                       CONSTRAINT seats_performance_label_unique
                           UNIQUE (performance_id, label),

                       CONSTRAINT seats_status_check
                           CHECK (status IN ('AVAILABLE', 'HELD', 'SOLD')),

                       CONSTRAINT seats_hold_consistency_check
                           CHECK (
                               (status = 'AVAILABLE' AND hold_id IS NULL)
                                   OR
                               (status IN ('HELD', 'SOLD') AND hold_id IS NOT NULL)
                               )
);

CREATE INDEX seats_performance_status_index
    ON seats (performance_id, status);

CREATE INDEX holds_active_expiry_index
    ON holds (status, expires_at);

INSERT INTO performances (name)
VALUES ('2026 Demo Concert');

INSERT INTO seats (performance_id, label, status)
SELECT
    1,
    'A-' || seat_number,
    'AVAILABLE'
FROM generate_series(1, 1000) AS seat_number;