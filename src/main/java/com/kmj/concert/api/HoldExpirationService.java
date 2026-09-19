package com.kmj.concert.api;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HoldExpirationService {

    private final JdbcTemplate jdbcTemplate;

    public HoldExpirationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void expireDueHolds() {
        jdbcTemplate.update("""
                WITH expired_holds AS (
                    UPDATE holds
                    SET status = 'EXPIRED'
                    WHERE status = 'ACTIVE'
                      AND expires_at <= CURRENT_TIMESTAMP
                    RETURNING id
                )
                UPDATE seats
                SET status = 'AVAILABLE',
                    hold_id = NULL
                WHERE status = 'HELD'
                  AND hold_id IN (SELECT id FROM expired_holds)
                """);
    }
}