package com.example.binance_backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "bot_state")
public class BotState {

    @Id
    @GeneratedValue
    private UUID id;

    // <<< Relacionamento ManyToOne para User >>>
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_bot_state_user")
    )
    private User user;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @UpdateTimestamp
    @Column(name = "last_updated", nullable = false)
    private OffsetDateTime lastUpdated;

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

    public boolean isActive() {
        return isActive;
    }
    public void setActive(boolean active) {
        isActive = active;
    }

    public OffsetDateTime getLastUpdated() {
        return lastUpdated;
    }
    public void setLastUpdated(OffsetDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}
