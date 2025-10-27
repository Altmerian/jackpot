package com.pshakhlovich.jackpot.api.dto;

import jakarta.validation.constraints.NotBlank;

public record JackpotEvaluationRequest(
        @NotBlank(message = "Bet ID is required")
        String betId,

        @NotBlank(message = "Jackpot ID is required")
        String jackpotId
) {
}
