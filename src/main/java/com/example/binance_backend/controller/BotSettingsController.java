package com.example.binance_backend.controller;

import com.example.binance_backend.dto.BotSettingsRequest;
import com.example.binance_backend.model.BotSettings;
import com.example.binance_backend.model.User;
import com.example.binance_backend.repository.BotSettingsRepository;
import com.example.binance_backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bot-settings")
public class BotSettingsController {

    private final BotSettingsRepository settingsRepo;
    private final UserRepository userRepo;

    public BotSettingsController(BotSettingsRepository settingsRepo, UserRepository userRepo) {
        this.settingsRepo = settingsRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/user/{userId}")
    public BotSettings getByUser(@PathVariable UUID userId) {
        User user = userRepo.findById(userId)
                  .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        return settingsRepo.findByUser(user)
                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping
    public BotSettings create(@RequestBody BotSettingsRequest req) {
        User user = userRepo.findById(req.getUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (settingsRepo.findByUser(user).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Configuraçoes ja existem para este user");
        }

        BotSettings s = new BotSettings();
        s.setUser(user);
        s.setTradingPair(req.getTradingPair());
        s.setOrderType(req.getOrderType());
        s.setTradeAmount(req.getTradeAmount());
        s.setLimitPrice(req.getLimitPrice());
        s.setStopPrice(req.getStopPrice());
        s.setTrailingDelta(req.getTrailingDelta());
        s.setStopLossPerc(req.getStopLossPerc());
        s.setTakeProfitPerc(req.getTakeProfitPerc());
        s.setRsiEnabled(req.isRsiEnabled());
        s.setRsiThreshold(req.getRsiThreshold());
        s.setMacdEnabled(req.isMacdEnabled());
        s.setMovingAvgEnabled(req.isMovingAvgEnabled());
        return settingsRepo.save(s);
    }

    @PutMapping("/user/{userId}")
    public BotSettings update(@PathVariable UUID userId,
                              @RequestBody BotSettingsRequest req) {
        User user = userRepo.findById(userId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        BotSettings existing = settingsRepo.findByUser(user)
                                   .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        existing.setTradingPair(req.getTradingPair());
        existing.setOrderType(req.getOrderType());
        existing.setTradeAmount(req.getTradeAmount());
        existing.setLimitPrice(req.getLimitPrice());
        existing.setStopPrice(req.getStopPrice());
        existing.setTrailingDelta(req.getTrailingDelta());
        existing.setStopLossPerc(req.getStopLossPerc());
        existing.setTakeProfitPerc(req.getTakeProfitPerc());
        existing.setRsiEnabled(req.isRsiEnabled());
        existing.setRsiThreshold(req.getRsiThreshold());
        existing.setMacdEnabled(req.isMacdEnabled());
        existing.setMovingAvgEnabled(req.isMovingAvgEnabled());
        return settingsRepo.save(existing);
    }
}
