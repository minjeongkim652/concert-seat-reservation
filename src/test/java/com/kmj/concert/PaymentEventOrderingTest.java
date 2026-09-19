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

class PaymentEventOrderingTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Autowired
    private PaymentEventService paymentEventService;

    @Test
    void cancelledEventArrivingBeforeOlderApprovedEventKeepsSeatsAvailable() {
        HoldResponse hold = holdService.createHold(
                new CreateHoldRequest(1L, "jisu", List.of("A-1", "A-2"),"req-payment-ordering")
        );

        PaymentEventRequest cancelled = new PaymentEventRequest(
                "evt-cancelled-first",
                hold.paymentId(),
                hold.holdId(),
                "cancelled",
                Instant.parse("2026-09-19T12:00:05Z")
        );

        PaymentEventRequest olderApproved = new PaymentEventRequest(
                "evt-approved-late",
                hold.paymentId(),
                hold.holdId(),
                "approved",
                Instant.parse("2026-09-19T12:00:00Z")
        );

        paymentEventService.receive(cancelled);
        paymentEventService.receive(olderApproved);

        String holdStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM holds WHERE id = ?",
                String.class,
                hold.holdId()
        );

        String paymentStatus = jdbcTemplate.queryForObject(
                "SELECT payment_status FROM holds WHERE id = ?",
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

        assertThat(holdStatus).isEqualTo("CANCELLED");
        assertThat(paymentStatus).isEqualTo("CANCELLED");
        assertThat(seatStatuses).containsExactly("AVAILABLE", "AVAILABLE");
    }
}