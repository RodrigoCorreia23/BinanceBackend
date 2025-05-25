package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotTrade;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BotTradeRepository extends JpaRepository<BotTrade, UUID> {
  List<BotTrade> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
