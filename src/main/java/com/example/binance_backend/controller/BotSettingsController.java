package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotSettings;
import com.example.binance_backend.repository.BotSettingsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-settings")
public class BotSettingsController {

    private final BotSettingsRepository repo;

    public BotSettingsController(BotSettingsRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    public BotSettings getByUser(@PathVariable UUID userId) {
        return repo.findByUserId(userId)
                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public BotSettings create(@RequestBody BotSettings s) {
        OffsetDateTime now = OffsetDateTime.now();
        s.setCreatedAt(now);
        s.setUpdatedAt(now);
        return repo.save(s);
    }

    @PutMapping("/user/{userId}")
    public BotSettings update(@PathVariable UUID userId,
                              @RequestBody BotSettings s) {
        BotSettings existing = repo.findByUserId(userId)
                                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        s.setId(existing.getId());
        s.setCreatedAt(existing.getCreatedAt());
        s.setUpdatedAt(OffsetDateTime.now());
        return repo.save(s);
    }
}
