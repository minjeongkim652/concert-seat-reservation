package com.kmj.concert.api;

import com.kmj.concert.domain.Seat;
import com.kmj.concert.domain.SeatStatus;

public record SeatResponse(
        Long seatId,
        String label,
        SeatStatus status
) {
    public static SeatResponse from(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getLabel(),
                seat.getStatus()
        );
    }
}