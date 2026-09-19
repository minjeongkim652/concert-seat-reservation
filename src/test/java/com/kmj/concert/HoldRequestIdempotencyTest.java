package com.kmj.concert;

import com.kmj.concert.api.CreateHoldRequest;
import com.kmj.concert.api.HoldResponse;
import com.kmj.concert.api.HoldService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

class HoldRequestIdempotencyTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Test
    void sameRequestIdCreatesOnlyOneHold() throws Exception {
        CreateHoldRequest request = new CreateHoldRequest(
                1L,
                "minsu",
                List.of("A-1", "A-2"),
                "req-duplicate-hold"
        );

        int requestCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<HoldResponse>> futures = java.util.stream.IntStream
                    .range(0, requestCount)
                    .mapToObj(index -> executor.<HoldResponse>submit(() -> {
                        ready.countDown();
                        start.await();

                        return holdService.createHold(request);
                    }))
                    .toList();

            ready.await();
            start.countDown();

            List<HoldResponse> responses = futures.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    })
                    .toList();

            List<UUID> holdIds = responses.stream()
                    .map(HoldResponse::holdId)
                    .toList();

            Integer holdCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM holds WHERE request_id = ?",
                    Integer.class,
                    "req-duplicate-hold"
            );

            assertThat(holdIds).containsOnly(holdIds.get(0));
            assertThat(holdCount).isEqualTo(1);
        } finally {
            executor.shutdown();
        }
    }
}