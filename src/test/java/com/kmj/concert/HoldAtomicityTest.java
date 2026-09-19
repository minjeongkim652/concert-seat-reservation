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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HoldAtomicityTest extends IntegrationTestSupport {

    @Autowired
    private HoldService holdService;

    @Autowired
    private SeatRepository seatRepository;

    @Test
    void 여러_좌석_중_하나가_불가능하면_어느_좌석도_새로_선점되지_않는다() {
        holdService.createHold(new CreateHoldRequest(
                1L,
                "jisu",
                List.of("A-2"),
                "req-atomic-jisu"
        ));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> holdService.createHold(new CreateHoldRequest(
                        1L,
                        "minsu",
                        List.of("A-1", "A-2"),
                        "req-atomic-minsu"
                ))
        );

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        Seat a1 = findSeat("A-1");
        Seat a2 = findSeat("A-2");

        assertThat(a1.getStatus()).isEqualTo(SeatStatus.AVAILABLE);
        assertThat(a1.getHoldId()).isNull();

        assertThat(a2.getStatus()).isEqualTo(SeatStatus.HELD);
        assertThat(a2.getHoldId()).isNotNull();
    }

    private Seat findSeat(String label) {
        return seatRepository
                .findAllByPerformance_IdOrderById(1L)
                .stream()
                .filter(seat -> seat.getLabel().equals(label))
                .findFirst()
                .orElseThrow();
    }
}