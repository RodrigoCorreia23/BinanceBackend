package com.example.binance_backend.dto;
import java.math.BigDecimal;

public class UserProfileResponse {
    private String userId;
    private String username;
    private String email;
    private BigDecimal balance;


    public UserProfileResponse() { }

    public UserProfileResponse(String userId, String username, String email, BigDecimal balance) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.balance = balance;
    }

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public BigDecimal getBalance() {
        return balance;
    }
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
