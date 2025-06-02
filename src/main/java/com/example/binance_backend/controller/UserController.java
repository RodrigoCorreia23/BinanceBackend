package com.example.binance_backend.controller;

import com.example.binance_backend.dto.SignUpRequest;
import com.example.binance_backend.dto.SignUpResponse;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

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
        user.setPasswordHash(  // se quiser já guardar a hash na mesma tabela
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
}
