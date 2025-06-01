package com.example.binance_backend.controller;

import com.example.binance_backend.dto.ApiCredentialsRequest;
import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.UserCredentialsRepository;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.binance_backend.service.BinanceClient;


import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/credentials")
public class UserCredentialsController {

    private final UserRepository userRepo;
    private final UserCredentialsRepository credRepo;
    private final BinanceClient binanceClient; 

    public UserCredentialsController(UserRepository userRepo,
                                     UserCredentialsRepository credRepo,
                                     BinanceClient binanceClient) {
        this.userRepo = userRepo;
        this.credRepo = credRepo;
        this.binanceClient = binanceClient;
    }

    @PostMapping
    public ResponseEntity<?> saveCredentials(@RequestBody ApiCredentialsRequest req) {
        UUID userId;
        try {
            userId = UUID.fromString(req.getUserId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "ID de usuário inválido"));
        }

        if (userRepo.findById(userId).isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuário não encontrado"));
        }

        UserCredentials creds = new UserCredentials();
        creds.setUserId(userId);
        creds.setEncryptedApiKey(req.getApiKey());
        creds.setEncryptedSecretKey(req.getSecretKey());
        creds.setCreatedAt(OffsetDateTime.now());
        credRepo.save(creds);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

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
        boolean exists = credRepo.findByUserId(id).isPresent();
        return ResponseEntity.ok(Map.of("hasCredentials", exists));
    }

    @GetMapping("/{userId}/balance")
    public ResponseEntity<?> getBalance(@PathVariable String userId) {
        UUID id;
        try {
            id = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .badRequest()
                .body(Map.of("message", "ID inválido"));
        }

        Optional<UserCredentials> credOpt = credRepo.findByUserId(id);
        if (credOpt.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Credenciais não encontradas"));
        }

        UserCredentials creds = credOpt.get();

        // Aqui descriptografa se necessário e busca na Binance o saldo USDT
        String freeUsdt = binanceClient.fetchFreeBalance(
            creds.getEncryptedApiKey(),
            creds.getEncryptedSecretKey(),
            "USDT"
        );

        return ResponseEntity.ok(Map.of("free", freeUsdt));
    }

}
