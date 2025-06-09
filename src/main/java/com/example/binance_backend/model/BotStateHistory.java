package com.example.binance_backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "bot_state_history")
public class BotStateHistory {

    @Id
    @GeneratedValue
    private UUID id;

    // <<< Relacionamento ManyToOne para BotState >>>
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "bot_state_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bot_state_history_state")
    )
    private BotState botState;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private OffsetDateTime changedAt;

    @Column(name = "from_active", nullable = false)
    private boolean fromActive;

    @Column(name = "to_active", nullable = false)
    private boolean toActive;

    // ======= GETTERS & SETTERS =======

    public UUID getId() {
        return id;
    }
    public void setId(UUID id) {
        this.id = id;
    }

    public BotState getBotState() {
        return botState;
    }
    public void setBotState(BotState botState) {
        this.botState = botState;
    }

    public OffsetDateTime getChangedAt() {
        return changedAt;
    }

    public boolean isFromActive() {
        return fromActive;
    }
    public void setFromActive(boolean fromActive) {
        this.fromActive = fromActive;
    }

    public boolean isToActive() {
        return toActive;
    }
    public void setToActive(boolean toActive) {
        this.toActive = toActive;
    }
}
