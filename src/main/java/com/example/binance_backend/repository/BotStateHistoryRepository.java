package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotStateHistory;
import com.example.binance_backend.model.BotState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BotStateHistoryRepository extends JpaRepository<BotStateHistory, UUID> {
    List<BotStateHistory> findByBotStateOrderByChangedAtDesc(BotState botState);
}
