package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotStateHistory;
import com.example.binance_backend.repository.BotStateHistoryRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-state-history")
public class BotStateHistoryController {

    private final BotStateHistoryRepository repo;

    public BotStateHistoryController(BotStateHistoryRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/state/{botStateId}")
    public List<BotStateHistory> listByState(@PathVariable UUID botStateId) {
        return repo.findByBotStateIdOrderByChangedAtDesc(botStateId);
    }

    @PostMapping
    public BotStateHistory create(@RequestBody BotStateHistory h) {
        // assume que h.changedAt está preenchido pelo chamador
        return repo.save(h);
    }
}
