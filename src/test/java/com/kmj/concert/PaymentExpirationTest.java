package com.kmj.concert;

import com.kmj.concert.api.CreateHoldRequest;
import com.kmj.concert.api.HoldResponse;
import com.kmj.concert.api.HoldService;
import com.kmj.concert.api.PaymentEventRequest;
import com.kmj.concert.api.PaymentEventService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentExpirationTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Autowired
    private PaymentEventService paymentEventService;

    @Test
    void expirationWinsWhenApprovalArrivesAtExpirationTime() {
        HoldResponse hold = holdService.createHold(
                new CreateHoldRequest(1L, "minsu", List.of("A-1", "A-2"),"req-payment-expiration")
        );

        jdbcTemplate.update(
                "UPDATE holds SET expires_at = CURRENT_TIMESTAMP WHERE id = ?",
                hold.holdId()
        );

        PaymentEventRequest approved = new PaymentEventRequest(
                "evt-approved-at-expiration",
                hold.paymentId(),
                hold.holdId(),
                "approved",
                Instant.parse("2026-09-19T12:00:00Z")
        );

        paymentEventService.receive(approved);

        String holdStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM holds WHERE id = ?",
                String.class,
                hold.holdId()
        );

        List<String> seatStatuses = jdbcTemplate.queryForList(
                """
                SELECT status
                FROM seats
                WHERE performance_id = 1
                  AND label IN ('A-1', 'A-2')
                ORDER BY label
                """,
                String.class
        );

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_events WHERE event_id = ?",
                Integer.class,
                "evt-approved-at-expiration"
        );

        assertThat(holdStatus).isEqualTo("EXPIRED");
        assertThat(seatStatuses).containsExactly("AVAILABLE", "AVAILABLE");
        assertThat(eventCount).isEqualTo(1);
    }
}