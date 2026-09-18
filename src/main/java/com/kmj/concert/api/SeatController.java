package com.kmj.concert.api;

import com.kmj.concert.domain.PerformanceRepository;
import com.kmj.concert.domain.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/performances")
public class SeatController {

    private final PerformanceRepository performanceRepository;
    private final SeatRepository seatRepository;

    public SeatController(
            PerformanceRepository performanceRepository,
            SeatRepository seatRepository
    ) {
        this.performanceRepository = performanceRepository;
        this.seatRepository = seatRepository;
    }

    @GetMapping("/{performanceId}/seats")
    public List<SeatResponse> getSeats(
            @PathVariable Long performanceId
    ) {
        if (!performanceRepository.existsById(performanceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "공연을 찾을 수 없습니다."
            );
        }

        return seatRepository
                .findAllByPerformance_IdOrderById(performanceId)
                .stream()
                .map(SeatResponse::from)
                .toList();
    }
}