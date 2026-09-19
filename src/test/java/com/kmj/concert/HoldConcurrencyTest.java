package com.kmj.concert;

import com.kmj.concert.api.CreateHoldRequest;
import com.kmj.concert.api.HoldService;
import com.kmj.concert.domain.Seat;
import com.kmj.concert.domain.SeatRepository;
import com.kmj.concert.domain.SeatStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class HoldConcurrencyTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void 같은_좌석에_동시_선점_요청이_오면_하나만_성공한다() throws Exception {
        int requestCount = 20;

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);

        try {
            List<Future<Boolean>> results = java.util.stream.IntStream
                    .range(0, requestCount)
                    .mapToObj(index -> executor.submit(() -> {
                        ready.countDown();
                        start.await();

                        try {
                            holdService.createHold(new CreateHoldRequest(
                                    1L,
                                    "user-" + index,
                                    List.of("A-1"),
                                    "req-concurrent-" + index
                            ));
                            return true;
                        } catch (ResponseStatusException exception) {
                            if (exception.getStatusCode() == HttpStatus.CONFLICT) {
                                return false;
                            }
                            throw exception;
                        }
                    }))
                    .toList();

            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

            start.countDown();

            long successCount = 0;

            for (Future<Boolean> result : results) {
                if (result.get(10, TimeUnit.SECONDS)) {
                    successCount++;
                }
            }

            assertThat(successCount).isEqualTo(1);

            Seat seat = seatRepository
                    .findAllByPerformance_IdOrderById(1L)
                    .stream()
                    .filter(candidate -> candidate.getLabel().equals("A-1"))
                    .findFirst()
                    .orElseThrow();

            assertThat(seat.getStatus()).isEqualTo(SeatStatus.HELD);
            assertThat(seat.getHoldId()).isNotNull();
        } finally {
            executor.shutdownNow();
        }
    }
}