package com.pshakhlovich.jackpot.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record BetRequest(
        @NotBlank(message = "betId is required") String betId,
        @NotBlank(message = "userId is required") String userId,
        @NotBlank(message = "jackpotId is required") String jackpotId,
        @NotNull(message = "betAmount is required") @Positive(message = "betAmount must be positive") BigDecimal betAmount
) {
}
