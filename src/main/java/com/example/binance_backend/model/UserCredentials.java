package com.example.binance_backend.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "user_credentials",
    uniqueConstraints = @UniqueConstraint(columnNames = "user_id")
)
public class UserCredentials {

    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_user_credentials_user")
    )
    private User user;

    @Column(name = "encrypted_api_key", nullable = false)
    private String encryptedApiKey;

    @Column(name = "encrypted_secret_key", nullable = false)
    private String encryptedSecretKey;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "rotated_at")
    private OffsetDateTime rotatedAt;

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

    public String getEncryptedApiKey() {
        return encryptedApiKey;
    }
    public void setEncryptedApiKey(String encryptedApiKey) {
        this.encryptedApiKey = encryptedApiKey;
    }

    public String getEncryptedSecretKey() {
        return encryptedSecretKey;
    }
    public void setEncryptedSecretKey(String encryptedSecretKey) {
        this.encryptedSecretKey = encryptedSecretKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getRotatedAt() {
        return rotatedAt;
    }
    public void setRotatedAt(OffsetDateTime rotatedAt) {
        this.rotatedAt = rotatedAt;
    }
}
