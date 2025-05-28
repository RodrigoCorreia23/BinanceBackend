// src/main/java/com/example/binance_backend/controller/AuthController.java
package com.example.binance_backend.controller;

import com.example.binance_backend.dto.LoginRequest;
import com.example.binance_backend.dto.LoginResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import com.example.binance_backend.repository.UserRepository;
import com.example.binance_backend.repository.UserCredentialsRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepo;
    private final UserCredentialsRepository credRepo;

    public AuthController(UserRepository userRepo,
                          UserCredentialsRepository credRepo) {
        this.userRepo = userRepo;
        this.credRepo = credRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        Optional<User> userOpt = userRepo.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Credenciais inválidas"));
        }
        User user = userOpt.get();

        Optional<UserCredentials> credOpt = credRepo.findByUserId(user.getId());
        if (credOpt.isEmpty() ||
            !BCrypt.checkpw(req.getPassword(), credOpt.get().getPasswordHash())) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Credenciais inválidas"));
        }

        LoginResponse resp = new LoginResponse(
            user.getId().toString(),
            user.getUsername()
        );
        return ResponseEntity.ok(resp);
    }
}
