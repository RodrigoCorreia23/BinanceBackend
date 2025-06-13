package com.example.binance_backend.controller;

import com.example.binance_backend.dto.*;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import com.example.binance_backend.dto.AddFundsRequest;  

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserRepository userRepo;

    public UserController(UserRepository userRepo) {
        this.userRepo = userRepo;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignUpRequest req) {
        Map<String,String> errors = new HashMap<>();
        if (userRepo.existsByEmail(req.getEmail())) {
            errors.put("email", "Email já existe");
        }
        if (userRepo.existsByUsername(req.getUsername())) {
            errors.put("username", "Nome de utilizador já existe.");
        }
        if (!errors.isEmpty()) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errors);
        }

        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordHash(BCrypt.hashpw(req.getPassword(), BCrypt.gensalt()));
        user = userRepo.save(user);

        SignUpResponse resp = new SignUpResponse(
            user.getId().toString(),
            user.getUsername()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/{id}")
public ResponseEntity<?> getUserProfile(@PathVariable("id") String userId) {
    UUID uuid;
    try {
        uuid = UUID.fromString(userId);
    } catch (IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Map.of("message", "ID do utilizador inválido"));
    }

    Optional<User> userOpt = userRepo.findById(uuid);
    if (userOpt.isEmpty()) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "User não encontrado"));
    }

    User u = userOpt.get();
    UserProfileResponse profile = new UserProfileResponse(
        u.getId().toString(),
        u.getUsername(),
        u.getEmail(),
        u.getBalance()
    );
    return ResponseEntity.ok(profile);
}

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(
        @PathVariable String id,
        @RequestBody UpdateProfileRequest updateReq
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "ID de utilizador inválido"));
        }

        return userRepo.findById(uuid)
            .map(user -> {
                String novoEmail    = updateReq.getEmail().trim();
                String novoUsername = updateReq.getUsername().trim();

                if (!novoEmail.equalsIgnoreCase(user.getEmail())
                    && userRepo.existsByEmail(novoEmail)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("email", "Email já está em uso"));
                }
                if (!novoUsername.equalsIgnoreCase(user.getUsername())
                    && userRepo.existsByUsername(novoUsername)) {
                    return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("username", "Username já está em uso"));
                }

                user.setEmail(novoEmail);
                user.setUsername(novoUsername);
                userRepo.save(user);

                UserProfileResponse updated = new UserProfileResponse(
                    user.getId().toString(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getBalance()
                );
                return ResponseEntity.ok(updated);
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User não encontrado")));
    }

    @PutMapping("/{id}/add-funds")
    public ResponseEntity<?> addFunds(
        @PathVariable String id,
        @RequestBody AddFundsRequest req
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "ID do utilizador inválido"));
        }

        BigDecimal amount = req.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest()
                .body(Map.of("message", "Valor inválido"));
        }

        return userRepo.findById(uuid)
            .map(user -> {
                BigDecimal newBalance = user.getBalance().add(amount);
                user.setBalance(newBalance);
                userRepo.save(user);

                return ResponseEntity.ok(Map.of(
                    "message", "Fundos adicionados com sucesso",
                    "newBalance", newBalance
                ));
            })
            .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "User não encontrado")));
    }
}
