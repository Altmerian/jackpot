package com.pshakhlovich.jackpot.api;

import com.pshakhlovich.jackpot.api.dto.JackpotEvaluationResponse;
import com.pshakhlovich.jackpot.service.JackpotEvaluationService;
import com.pshakhlovich.jackpot.service.dto.RewardResult;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/evaluations")
@Validated
@RequiredArgsConstructor
public class JackpotEvaluationController {

    private final JackpotEvaluationService evaluationService;

    @GetMapping
    public ResponseEntity<JackpotEvaluationResponse> evaluate(
            @RequestParam @NotBlank(message = "betId is required") String betId,
            @RequestParam @NotBlank(message = "jackpotId is required") String jackpotId) {

        RewardResult result = evaluationService.evaluate(betId, jackpotId);

        JackpotEvaluationResponse response = new JackpotEvaluationResponse(
                result.win(),
                result.payoutAmount(),
                result.updatedPool(),
                result.probability(),
                result.strategy(),
                betId,
                jackpotId
        );

        return ResponseEntity.ok(response);
    }
}
