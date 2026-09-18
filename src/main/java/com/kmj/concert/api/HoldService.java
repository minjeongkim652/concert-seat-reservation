package com.kmj.concert.api;

import com.kmj.concert.domain.Hold;
import com.kmj.concert.domain.HoldRepository;
import com.kmj.concert.domain.Performance;
import com.kmj.concert.domain.PerformanceRepository;
import com.kmj.concert.domain.Seat;
import com.kmj.concert.domain.SeatRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class HoldService {

    private static final Duration HOLD_DURATION = Duration.ofMinutes(5);

    private final PerformanceRepository performanceRepository;
    private final SeatRepository seatRepository;
    private final HoldRepository holdRepository;

    public HoldService(
            PerformanceRepository performanceRepository,
            SeatRepository seatRepository,
            HoldRepository holdRepository
    ) {
        this.performanceRepository = performanceRepository;
        this.seatRepository = seatRepository;
        this.holdRepository = holdRepository;
    }

    @Transactional
    public HoldResponse createHold(CreateHoldRequest request) {
        Set<String> uniqueLabels = new LinkedHashSet<>(request.seatLabels());

        if (uniqueLabels.size() != request.seatLabels().size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "같은 좌석을 중복해서 요청할 수 없습니다."
            );
        }

        Performance performance = performanceRepository
                .findById(request.performanceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "공연을 찾을 수 없습니다."
                ));

        List<Seat> seats = seatRepository.findAllForUpdate(
                performance.getId(),
                List.copyOf(uniqueLabels)
        );

        if (seats.size() != uniqueLabels.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 좌석이 포함되어 있습니다."
            );
        }

        List<String> unavailableSeats = seats.stream()
                .filter(seat -> !seat.isAvailable())
                .map(Seat::getLabel)
                .toList();

        if (!unavailableSeats.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 선점되었거나 판매된 좌석이 있습니다: " + unavailableSeats
            );
        }

        Instant now = Instant.now();

        Hold hold = Hold.createActive(
                performance,
                request.userId(),
                now,
                HOLD_DURATION
        );

        holdRepository.save(hold);

        seats.forEach(seat -> seat.hold(hold.getId()));

        return new HoldResponse(
                hold.getId(),
                seats.stream().map(Seat::getLabel).toList(),
                hold.getExpiresAt()
        );
    }
    @Transactional
    public void cancelHold(UUID holdId, String userId) {
        Hold hold = holdRepository.findByIdForUpdate(holdId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "선점을 찾을 수 없습니다."
                ));

        if (!hold.getUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "본인이 만든 선점만 취소할 수 있습니다."
            );
        }

        if (!hold.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 취소되었거나 종료된 선점입니다."
            );
        }

        List<Seat> seats = seatRepository.findAllByHoldIdForUpdate(holdId);

        hold.cancel();
        seats.forEach(Seat::release);
    }
}