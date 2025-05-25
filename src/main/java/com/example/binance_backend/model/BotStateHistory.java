package com.example.binance_backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "bot_state_history")
public class BotStateHistory {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "bot_state_id", nullable = false)
    private UUID botStateId;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;

    @Column(name = "from_active", nullable = false)
    private boolean fromActive;

    @Column(name = "to_active", nullable = false)
    private boolean toActive;

    // getters & setters

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getBotStateId() { return botStateId; }
    public void setBotStateId(UUID botStateId) { this.botStateId = botStateId; }

    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }

    public boolean isFromActive() { return fromActive; }
    public void setFromActive(boolean fromActive) { this.fromActive = fromActive; }

    public boolean isToActive() { return toActive; }
    public void setToActive(boolean toActive) { this.toActive = toActive; }
}
