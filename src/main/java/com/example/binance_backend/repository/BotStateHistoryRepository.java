package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotStateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BotStateHistoryRepository extends JpaRepository<BotStateHistory, UUID> {
  List<BotStateHistory> findByBotStateIdOrderByChangedAtDesc(UUID botStateId);
}
