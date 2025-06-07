package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotTrade;
import com.example.binance_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BotTradeRepository extends JpaRepository<BotTrade, UUID> {

    @Query("SELECT t FROM BotTrade t " +
           "WHERE t.user.id = :userId " +
           "  AND t.symbol = :symbol " +
           "  AND t.status = 'OPEN'")
    Optional<BotTrade> findOpenTradeByUserAndSymbol(
            @Param("userId") UUID userId,
            @Param("symbol") String symbol
    );

    List<BotTrade> findByUserOrderByCreatedAtDesc(User user); // usa o User direto
    List<BotTrade> findByUserIdOrderByCreatedAtDesc(UUID userId); // usa o ID do User
}
