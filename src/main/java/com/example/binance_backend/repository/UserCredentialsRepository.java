package com.example.binance_backend.repository;

import com.example.binance_backend.model.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
  Optional<UserCredentials> findByUserId(UUID userId);
}
