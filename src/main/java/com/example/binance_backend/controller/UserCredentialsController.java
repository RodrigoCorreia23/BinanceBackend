// src/main/java/com/example/binance_backend/controller/UserCredentialsController.java
package com.example.binance_backend.controller;

import com.example.binance_backend.dto.ApiCredentialsRequest;
import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.UserCredentialsRepository;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/credentials")
public class UserCredentialsController {

    private final UserRepository userRepo;
    private final UserCredentialsRepository credRepo;

    public UserCredentialsController(UserRepository userRepo,
                                     UserCredentialsRepository credRepo) {
        this.userRepo = userRepo;
        this.credRepo = credRepo;
    }

    @PostMapping
    public ResponseEntity<?> saveCredentials(@RequestBody ApiCredentialsRequest req) {
        // 1) valida se o user existe
        UUID userId;
        try {
            userId = UUID.fromString(req.getUserId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "ID de usuário inválido"));
        }

        Optional<User> userOpt = userRepo.findById(userId);
        if (userOpt.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuário não encontrado"));
        }

        // 2) monta e persiste UserCredentials
        UserCredentials creds = new UserCredentials();
        creds.setUserId(userId);
        // aqui você pode cifrar as chaves antes de salvar, se quiser
        creds.setEncryptedApiKey(req.getApiKey());
        creds.setEncryptedSecretKey(req.getSecretKey());
        creds.setCreatedAt(OffsetDateTime.now());
        // se você tiver um campo rotatedAt, pode deixar nulo
        credRepo.save(creds);

        // 3) devolve 201 Created sem corpo
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
