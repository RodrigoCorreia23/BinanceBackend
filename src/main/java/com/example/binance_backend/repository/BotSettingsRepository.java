package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotSettings;
import com.example.binance_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotSettingsRepository extends JpaRepository<BotSettings, UUID> {
    Optional<BotSettings> findByUser(User user);
}
