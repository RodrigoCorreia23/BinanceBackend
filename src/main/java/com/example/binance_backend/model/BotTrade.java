package com.example.binance_backend.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bot_trades")
public class BotTrade {

    @Id
    @GeneratedValue
    private UUID id;

    // <<< Relacionamento ManyToOne para User >>>
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bot_trade_user")
    )
    private User user;

    @Column(nullable = false)
    private String symbol;

    @Column(nullable = false)
    private String side;  // "buy" / "sell"

    @Column(precision = 16, scale = 8)
    private BigDecimal amount;

    @Column(precision = 16, scale = 8)
    private BigDecimal price;

    @Column(precision = 16, scale = 8)
    private BigDecimal fee;

    @Column(name = "profit_estimate", precision = 16, scale = 8)
    private BigDecimal profitEstimate;

    @Column(nullable = false)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "executed_at")
    private OffsetDateTime executedAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    // ======= GETTERS & SETTERS =======

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }
    public void setUser(User user) {
        this.user = user;
    }

    public String getSymbol() {
        return symbol;
    }
    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getSide() {
        return side;
    }
    public void setSide(String side) {
        this.side = side;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPrice() {
        return price;
    }
    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getFee() {
        return fee;
    }
    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getProfitEstimate() {
        return profitEstimate;
    }
    public void setProfitEstimate(BigDecimal profitEstimate) {
        this.profitEstimate = profitEstimate;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
    // Não há setter para createdAt, pois é automático com @CreationTimestamp

    public OffsetDateTime getExecutedAt() {
        return executedAt;
    }
    public void setExecutedAt(OffsetDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
