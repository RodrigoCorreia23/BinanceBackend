package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BotSettingsRepository extends JpaRepository<BotSettings, UUID> {
  Optional<BotSettings> findByUserId(UUID userId);
}
