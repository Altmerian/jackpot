package com.pshakhlovich.jackpot.api;

import com.pshakhlovich.jackpot.api.dto.BetRequest;
import com.pshakhlovich.jackpot.api.dto.BetResponse;
import com.pshakhlovich.jackpot.service.BetPublishingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bets")
@Validated
@RequiredArgsConstructor
public class BetController {

    private final BetPublishingService betPublishingService;

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public BetResponse publishBet(@Valid @RequestBody BetRequest request) {
        String betId = betPublishingService.publish(request);
        return new BetResponse(betId);
    }
}
