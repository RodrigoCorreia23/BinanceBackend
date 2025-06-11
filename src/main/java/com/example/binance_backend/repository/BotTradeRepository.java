package com.example.binance_backend.repository;

import com.example.binance_backend.model.BotTrade;
import com.example.binance_backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BotTradeRepository extends JpaRepository<BotTrade, UUID> {

    @Query("SELECT t FROM BotTrade t " +
           "WHERE t.user.id = :userId " +
           "  AND t.symbol = :symbol " +
           "  AND t.status = 'OPEN'")
    Optional<BotTrade> findOpenTradeByUserAndSymbol(
            @Param("userId") UUID userId,
            @Param("symbol") String symbol
    );

    // Busca todos os trades (abertos e fechados) de um usuário, ordenados por data decrescente
    List<BotTrade> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Caso prefira usar o objeto User diretamente
    List<BotTrade> findByUserOrderByCreatedAtDesc(User user);
}
