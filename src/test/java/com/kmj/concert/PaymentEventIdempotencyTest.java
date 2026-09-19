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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentEventIdempotencyTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Autowired
    private PaymentEventService paymentEventService;

    @Test
    void samePaymentEventIsProcessedOnlyOnce() throws Exception {
        HoldResponse hold = holdService.createHold(
                new CreateHoldRequest(1L, "minsu", List.of("A-1", "A-2"),"req-payment-idempotency")
        );

        PaymentEventRequest event = new PaymentEventRequest(
                "evt-duplicate-001",
                hold.paymentId(),
                hold.holdId(),
                "approved",
                Instant.parse("2026-09-19T09:00:00Z")
        );

        int requestCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Void>> futures = java.util.stream.IntStream.range(0, requestCount)
                    .mapToObj(index -> executor.<Void>submit(() -> {
                        ready.countDown();
                        start.await();

                        paymentEventService.receive(event);
                        return null;
                    }))
                    .toList();

            ready.await();
            start.countDown();

            for (Future<Void> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdown();
        }

        Integer eventCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payment_events WHERE event_id = ?",
                Integer.class,
                "evt-duplicate-001"
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

        assertThat(eventCount).isEqualTo(1);
        assertThat(seatStatuses).containsExactly("SOLD", "SOLD");
    }
}
