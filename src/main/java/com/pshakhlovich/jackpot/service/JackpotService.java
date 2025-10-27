package com.pshakhlovich.jackpot.service;

import com.pshakhlovich.jackpot.domain.model.Jackpot;
import com.pshakhlovich.jackpot.repository.JackpotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class JackpotService {

    private final JackpotRepository jackpotRepository;

    @Transactional(value = "transactionManager", readOnly = true)
    public Jackpot getRequired(String jackpotId) {
        return jackpotRepository.findById(jackpotId)
                .orElseThrow(() -> new IllegalArgumentException("Jackpot %s not found".formatted(jackpotId)));
    }
}
