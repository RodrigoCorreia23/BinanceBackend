package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotState;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface BotStateRepository extends JpaRepository<BotState, UUID> {
  Optional<BotState> findByUserId(UUID userId);
}
