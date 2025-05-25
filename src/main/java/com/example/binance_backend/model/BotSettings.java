package com.example.binance_backend.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bot_settings")
public class BotSettings {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "trading_pair", nullable = false)
    private String tradingPair;

    @Column(name = "order_type", nullable = false)
    private String orderType;

    @Column(name = "trade_amount", precision = 16, scale = 8)
    private BigDecimal tradeAmount;

    @Column(name = "limit_price", precision = 16, scale = 8)
    private BigDecimal limitPrice;

    @Column(name = "stop_loss_perc", precision = 5, scale = 2)
    private BigDecimal stopLossPerc;

    @Column(name = "take_profit_perc", precision = 5, scale = 2)
    private BigDecimal takeProfitPerc;

    @Column(name = "rsi_enabled", nullable = false)
    private boolean rsiEnabled;

    @Column(name = "rsi_threshold")
    private Integer rsiThreshold;

    @Column(name = "macd_enabled", nullable = false)
    private boolean macdEnabled;

    @Column(name = "moving_avg_enabled", nullable = false)
    private boolean movingAvgEnabled;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // getters & setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTradingPair() { return tradingPair; }
    public void setTradingPair(String tradingPair) { this.tradingPair = tradingPair; }

    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }

    public BigDecimal getTradeAmount() { return tradeAmount; }
    public void setTradeAmount(BigDecimal tradeAmount) { this.tradeAmount = tradeAmount; }

    public BigDecimal getLimitPrice() { return limitPrice; }
    public void setLimitPrice(BigDecimal limitPrice) { this.limitPrice = limitPrice; }

    public BigDecimal getStopLossPerc() { return stopLossPerc; }
    public void setStopLossPerc(BigDecimal stopLossPerc) { this.stopLossPerc = stopLossPerc; }

    public BigDecimal getTakeProfitPerc() { return takeProfitPerc; }
    public void setTakeProfitPerc(BigDecimal takeProfitPerc) { this.takeProfitPerc = takeProfitPerc; }

    public boolean isRsiEnabled() { return rsiEnabled; }
    public void setRsiEnabled(boolean rsiEnabled) { this.rsiEnabled = rsiEnabled; }

    public Integer getRsiThreshold() { return rsiThreshold; }
    public void setRsiThreshold(Integer rsiThreshold) { this.rsiThreshold = rsiThreshold; }

    public boolean isMacdEnabled() { return macdEnabled; }
    public void setMacdEnabled(boolean macdEnabled) { this.macdEnabled = macdEnabled; }

    public boolean isMovingAvgEnabled() { return movingAvgEnabled; }
    public void setMovingAvgEnabled(boolean movingAvgEnabled) { this.movingAvgEnabled = movingAvgEnabled; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
