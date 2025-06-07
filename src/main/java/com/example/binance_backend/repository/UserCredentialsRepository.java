package com.example.binance_backend.repository;

import com.example.binance_backend.model.User;
import com.example.binance_backend.model.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserCredentialsRepository extends JpaRepository<UserCredentials, UUID> {
    Optional<UserCredentials> findByUser(User user);
    boolean existsByUser(User user);
}
