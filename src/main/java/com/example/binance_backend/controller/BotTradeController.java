package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotTrade;
import com.example.binance_backend.repository.BotTradeRepository;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-trades")
public class BotTradeController {

    private final BotTradeRepository repo;

    public BotTradeController(BotTradeRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    public List<BotTrade> listByUser(@PathVariable UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @PostMapping
    public BotTrade create(@RequestBody BotTrade t) {
        t.setCreatedAt(OffsetDateTime.now());
        return repo.save(t);
    }
}
