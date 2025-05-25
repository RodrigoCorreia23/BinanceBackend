package com.example.binance_backend.controller;

import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserRepository repo;
  public UserController(UserRepository repo) { this.repo = repo; }

  @GetMapping("/{id}")
  public User get(@PathVariable UUID id) {
    return repo.findById(id)
               .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
  }

  @PostMapping
  public User create(@RequestBody User u) {
    OffsetDateTime now = OffsetDateTime.now();
    u.setCreatedAt(now);
    u.setUpdatedAt(now);
    return repo.save(u);
  }

  @PutMapping("/{id}")
  public User update(@PathVariable UUID id, @RequestBody User u) {
    User exist = repo.findById(id)
                     .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    u.setId(id);
    u.setCreatedAt(exist.getCreatedAt());
    u.setUpdatedAt(OffsetDateTime.now());
    return repo.save(u);
  }
}
