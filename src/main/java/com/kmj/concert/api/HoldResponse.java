package com.kmj.concert.api;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record HoldResponse(
        UUID holdId,
        List<String> seatLabels,
        Instant expiresAt
) {
}