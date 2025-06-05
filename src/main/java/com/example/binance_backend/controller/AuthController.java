package com.example.binance_backend.controller;

import com.example.binance_backend.dto.LoginRequest;
import com.example.binance_backend.dto.LoginResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
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

    public AuthController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        // 1) tenta carregar usuário por e-mail
        Optional<User> userOpt = userRepo.findByEmail(req.getEmail());
        if (userOpt.isEmpty()) {
            // retorna 404 + {"email":"Email não cadastrado"}
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("email", "Email não cadastrado"));
        }
        User user = userOpt.get();

        // 2) compara senha crua com hash
        if (!BCrypt.checkpw(req.getPassword(), user.getPasswordHash())) {
            // retorna 401 + {"password":"Senha incorreta"}
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("password", "Senha incorreta"));
        }

        // 3) sucesso
        LoginResponse resp = new LoginResponse(
            user.getId().toString(),
            user.getUsername(),
            user.getEmail()
        );
        return ResponseEntity.ok(resp);
    }
}
