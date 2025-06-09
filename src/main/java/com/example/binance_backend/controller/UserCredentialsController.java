package com.example.binance_backend.controller;

import com.example.binance_backend.dto.ApiCredentialsRequest;
import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.UserCredentialsRepository;
import com.example.binance_backend.repository.UserRepository;
import com.example.binance_backend.service.BinanceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/credentials")
public class UserCredentialsController {

    private final UserRepository userRepo;
    private final UserCredentialsRepository credRepo;
    private final BinanceClient binanceClient;

    @Value("${bot.simulation:true}")
    private boolean simulationMode;

    public UserCredentialsController(UserRepository userRepo,
                                     UserCredentialsRepository credRepo,
                                     BinanceClient binanceClient) {
        this.userRepo = userRepo;
        this.credRepo = credRepo;
        this.binanceClient = binanceClient;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }

    // POST /api/credentials
    @PostMapping
    public ResponseEntity<?> saveCredentials(@RequestBody ApiCredentialsRequest req) {
        UUID userId;
        try {
            userId = UUID.fromString(req.getUserId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "ID de user invalido"));
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User nao encontrado"
                ));

        if (credRepo.existsByUser(user)) {
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Credenciais ja existentes para este user"));
        }

        UserCredentials creds = new UserCredentials();
        creds.setUser(user);
        creds.setEncryptedApiKey(req.getApiKey());
        creds.setEncryptedSecretKey(req.getSecretKey());
        // O createdAt é preenchido automaticamente
        credRepo.save(creds);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET /api/credentials/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<?> hasCredentials(@PathVariable String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "ID inválido"));
        }

        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User nao encontrado"
                ));

        boolean exists = credRepo.existsByUser(user);
        return ResponseEntity.ok(Map.of("hasCredentials", exists));
    }

    // GET /api/credentials/{userId}/balance
    @GetMapping("/{userId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", "ID invalido"));
        }

        User user = userRepo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "User nao encontrado"
                ));

        Optional<UserCredentials> credOpt = credRepo.findByUser(user);
        if (credOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Credenciais nao encontradas"));
        }

        if (simulationMode) {
            return ResponseEntity.ok(Map.of("free", "0"));
        }

        UserCredentials creds = credOpt.get();
        String freeUsdt = binanceClient.fetchFreeBalance(
            creds.getEncryptedApiKey(),
            creds.getEncryptedSecretKey(),
            "USDT"
        );
        return ResponseEntity.ok(Map.of("free", freeUsdt));
    }
}
