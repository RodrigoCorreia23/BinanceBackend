package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotState;
import com.example.binance_backend.repository.BotStateRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-state")
public class BotStateController {

    private final BotStateRepository repo;

    public BotStateController(BotStateRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    public BotState getByUser(@PathVariable UUID userId) {
        return repo.findByUserId(userId)
                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public BotState create(@RequestBody BotState s) {
        OffsetDateTime now = OffsetDateTime.now();
        s.setLastUpdated(now);
        return repo.save(s);
    }

    @PutMapping("/user/{userId}")
    public BotState update(@PathVariable UUID userId,
                           @RequestBody BotState s) {
        BotState existing = repo.findByUserId(userId)
                                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setId(existing.getId());
        // preserva createdAt? no BotState não há createdAt, só lastUpdated
        s.setLastUpdated(OffsetDateTime.now());
        return repo.save(s);
    }
}
