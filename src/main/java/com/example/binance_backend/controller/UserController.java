package com.example.binance_backend.controller;

import com.example.binance_backend.dto.SignUpRequest;
import com.example.binance_backend.dto.SignUpResponse;
import com.example.binance_backend.dto.UserProfileResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;
import com.example.binance_backend.dto.UpdateProfileRequest;


import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

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
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(errors);
        }

        // Só crio o User na tabela app_user
        User user = new User();
        user.setEmail(req.getEmail());
        user.setUsername(req.getUsername());
        user.setPasswordHash(
            BCrypt.hashpw(req.getPassword(), BCrypt.gensalt())
        );
        user = userRepo.save(user);

        SignUpResponse resp = new SignUpResponse(
            user.getId().toString(),
            user.getUsername()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resp);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable("id") String userId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(Map.of("message", "ID de usuário inválido"));
        }

        Optional<User> userOpt = userRepo.findById(uuid);
        if (userOpt.isEmpty()) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(Map.of("message", "Usuário não encontrado"));
        }

        User user = userOpt.get();
        UserProfileResponse profile = new UserProfileResponse(
            user.getId().toString(),
            user.getUsername(),
            user.getEmail()
        );
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUserProfile(
            @PathVariable("id") String userId,
            @RequestBody UpdateProfileRequest updateReq
    ) {
        UUID uuid;
        try {
            uuid = UUID.fromString(userId);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "ID de usuário inválido"));
        }

        Optional<User> userOpt = userRepo.findById(uuid);
        if (userOpt.isEmpty()) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Usuário não encontrado"));
        }

        // Validar se o novo email/username não colide com outro registro
        User user = userOpt.get();
        String novoEmail    = updateReq.getEmail().trim();
        String novoUsername = updateReq.getUsername().trim();

        // Se o email mudou, verificar existência
        if (!novoEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepo.existsByEmail(novoEmail)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("email", "Email já está em uso"));
            }
        }

        // Se o username mudou, verificar existência
        if (!novoUsername.equalsIgnoreCase(user.getUsername())) {
            if (userRepo.existsByUsername(novoUsername)) {
                return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(Map.of("username", "Nome de usuário já está em uso"));
            }
        }

        // De fato atualizar e salvar
        user.setEmail(novoEmail);
        user.setUsername(novoUsername);
        userRepo.save(user);

        // Retornar o perfil atualizado se quiser
        UserProfileResponse updatedProfile = new UserProfileResponse(
            user.getId().toString(),
            user.getUsername(),
            user.getEmail()
        );
        return ResponseEntity.ok(updatedProfile);
    }
}
