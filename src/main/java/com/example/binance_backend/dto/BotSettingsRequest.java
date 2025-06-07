package com.example.binance_backend.dto;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para receber JSON de BotSettings no controller.
 */
public class BotSettingsRequest {

    @NotNull
    private UUID userId;

    @NotNull
    private String tradingPair;

    @NotNull
    private String orderType;

    @NotNull
    private BigDecimal tradeAmount;

    private BigDecimal limitPrice;
    private BigDecimal stopPrice;
    private BigDecimal trailingDelta;

    private BigDecimal stopLossPerc;
    private BigDecimal takeProfitPerc;

    private boolean rsiEnabled;
    private Integer rsiThreshold;
    private boolean macdEnabled;
    private boolean movingAvgEnabled;

    // Getters e Setters

    public UUID getUserId() {
        return userId;
    }
    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTradingPair() {
        return tradingPair;
    }
    public void setTradingPair(String tradingPair) {
        this.tradingPair = tradingPair;
    }

    public String getOrderType() {
        return orderType;
    }
    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public BigDecimal getTradeAmount() {
        return tradeAmount;
    }
    public void setTradeAmount(BigDecimal tradeAmount) {
        this.tradeAmount = tradeAmount;
    }

    public BigDecimal getLimitPrice() {
        return limitPrice;
    }
    public void setLimitPrice(BigDecimal limitPrice) {
        this.limitPrice = limitPrice;
    }

    public BigDecimal getStopPrice() {
        return stopPrice;
    }
    public void setStopPrice(BigDecimal stopPrice) {
        this.stopPrice = stopPrice;
    }

    public BigDecimal getTrailingDelta() {
        return trailingDelta;
    }
    public void setTrailingDelta(BigDecimal trailingDelta) {
        this.trailingDelta = trailingDelta;
    }

    public BigDecimal getStopLossPerc() {
        return stopLossPerc;
    }
    public void setStopLossPerc(BigDecimal stopLossPerc) {
        this.stopLossPerc = stopLossPerc;
    }

    public BigDecimal getTakeProfitPerc() {
        return takeProfitPerc;
    }
    public void setTakeProfitPerc(BigDecimal takeProfitPerc) {
        this.takeProfitPerc = takeProfitPerc;
    }

    public boolean isRsiEnabled() {
        return rsiEnabled;
    }
    public void setRsiEnabled(boolean rsiEnabled) {
        this.rsiEnabled = rsiEnabled;
    }

    public Integer getRsiThreshold() {
        return rsiThreshold;
    }
    public void setRsiThreshold(Integer rsiThreshold) {
        this.rsiThreshold = rsiThreshold;
    }

    public boolean isMacdEnabled() {
        return macdEnabled;
    }
    public void setMacdEnabled(boolean macdEnabled) {
        this.macdEnabled = macdEnabled;
    }

    public boolean isMovingAvgEnabled() {
        return movingAvgEnabled;
    }
    public void setMovingAvgEnabled(boolean movingAvgEnabled) {
        this.movingAvgEnabled = movingAvgEnabled;
    }
}
