package com.kmj.concert.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public record PaymentEventRequest(
        @NotBlank String eventId,
        @NotBlank String paymentId,
        @NotNull UUID holdId,
        @NotBlank
        @Pattern(regexp = "approved|cancelled")
        String status,
        @NotNull Instant occurredAt
) {
}