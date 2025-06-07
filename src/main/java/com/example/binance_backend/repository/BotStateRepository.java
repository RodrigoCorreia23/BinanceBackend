package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotState;
import com.example.binance_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotStateRepository extends JpaRepository<BotState, UUID> {
    Optional<BotState> findByUser(User user);
    List<BotState> findAllByIsActiveTrue();
}
