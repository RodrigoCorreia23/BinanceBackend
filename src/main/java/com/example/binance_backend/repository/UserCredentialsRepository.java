// src/main/java/com/example/binance_backend/repository/UserCredentialsRepository.java
package com.example.binance_backend.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.binance_backend.model.UserCredentials;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
    /**
     * Carrega as credenciais associadas a um dado userId.
     */
    Optional<UserCredentials> findByUserId(UUID userId);
}
