package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotState;
import com.example.binance_backend.model.BotStateHistory;
import com.example.binance_backend.repository.BotStateHistoryRepository;
import com.example.binance_backend.repository.BotStateRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-state-history")
public class BotStateHistoryController {

    private final BotStateHistoryRepository historyRepo;
    private final BotStateRepository stateRepo;

    public BotStateHistoryController(BotStateHistoryRepository historyRepo,
                                     BotStateRepository stateRepo) {
        this.historyRepo = historyRepo;
        this.stateRepo = stateRepo;
    }

    @GetMapping("/state/{botStateId}")
    public List<BotStateHistory> listByState(@PathVariable UUID botStateId) {
        BotState botState = stateRepo.findById(botStateId)
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return historyRepo.findByBotStateOrderByChangedAtDesc(botState);
    }

    @PostMapping
    public BotStateHistory create(@RequestBody BotStateHistory h) {
        // Aqui h.getBotState() deve estar populado pelo payload JSON 
        // (o front-end envia apenas {"botState":{"id":"..."},"fromActive":true,"toActive":false})
        return historyRepo.save(h);
    }
}
