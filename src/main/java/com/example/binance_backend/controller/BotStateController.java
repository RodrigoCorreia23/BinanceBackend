package com.example.binance_backend.controller;

import com.example.binance_backend.model.BotState;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.BotSettingsRepository;
import com.example.binance_backend.repository.BotStateRepository;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot")
public class BotStateController {

    private final BotStateRepository botStateRepo;
    private final UserRepository userRepo;

    public BotStateController(BotStateRepository botStateRepo,
                              UserRepository userRepo) {
        this.botStateRepo = botStateRepo;
        this.userRepo = userRepo;
    }

    @PostMapping("/{userId}/activate")
    public ResponseEntity<?> activateBot(@PathVariable String userId) {
        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "ID invalido"));
        }

        User user = userRepo.findById(uid)
                    .orElse(null);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuario nao encontrado"));
        }

        Optional<BotState> maybeState = botStateRepo.findByUser(user);
        BotState state;
        if (maybeState.isPresent()) {
            state = maybeState.get();
            state.setActive(true);
            state.setLastUpdated(OffsetDateTime.now());
        } else {
            state = new BotState();
            state.setUser(user);
            state.setActive(true);
            state.setLastUpdated(OffsetDateTime.now());
        }
        botStateRepo.save(state);
        return ResponseEntity.ok(Map.of("message", "Bot ativado"));
    }

    @PostMapping("/{userId}/deactivate")
    public ResponseEntity<?> deactivateBot(@PathVariable String userId) {
        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "ID invalido"));
        }

        User user = userRepo.findById(uid)
                    .orElse(null);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User nao encontrado"));
        }

        Optional<BotState> maybeState = botStateRepo.findByUser(user);
        if (maybeState.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Estado do bot nao encontrado"));
        }

        BotState state = maybeState.get();
        state.setActive(false);
        state.setLastUpdated(OffsetDateTime.now());
        botStateRepo.save(state);
        return ResponseEntity.ok(Map.of("message", "Bot desativado"));
    }

    @GetMapping("/{userId}/status")
    public ResponseEntity<?> getBotStatus(@PathVariable String userId) {
        UUID uid;
        try {
            uid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "ID invalido"));
        }

        User user = userRepo.findById(uid)
                    .orElse(null);
        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User nao encontrado"));
        }

        Optional<BotState> maybeState = botStateRepo.findByUser(user);
        if (maybeState.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Estado do bot nao encontrado"));
        }

        boolean isActive = maybeState.get().isActive();
        return ResponseEntity.ok(Map.of("isActive", isActive));
    }
}
