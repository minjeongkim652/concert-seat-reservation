package com.kmj.concert.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateHoldRequest(
        @NotNull Long performanceId,
        @NotBlank String userId,
        @NotEmpty List<@NotBlank String> seatLabels
) {
}