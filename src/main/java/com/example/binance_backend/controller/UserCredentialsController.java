package com.example.binance_backend.controller;

import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.UserCredentialsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/credentials")
public class UserCredentialsController {

    private final UserCredentialsRepository repo;

    public UserCredentialsController(UserCredentialsRepository repo) {
        this.repo = repo;
    }

    @GetMapping("/user/{userId}")
    public UserCredentials getByUser(@PathVariable UUID userId) {
        return repo.findByUserId(userId)
                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public UserCredentials create(@RequestBody UserCredentials c) {
        OffsetDateTime now = OffsetDateTime.now();
        c.setCreatedAt(now);
        return repo.save(c);
    }

    @PutMapping("/user/{userId}")
    public UserCredentials update(@PathVariable UUID userId,
                                  @RequestBody UserCredentials c) {
        UserCredentials existing = repo.findByUserId(userId)
                                       .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        c.setId(existing.getId());
        c.setCreatedAt(existing.getCreatedAt());
        // a rotação da chave é manual, não alteramos createdAt
        return repo.save(c);
    }
}
