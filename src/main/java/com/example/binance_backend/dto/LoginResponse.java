// src/main/java/com/example/binance_backend/dto/LoginResponse.java
package com.example.binance_backend.dto;

public class LoginResponse {
    private String userId;
    private String username;

    public LoginResponse() { }

    public LoginResponse(String userId, String username) {
        this.userId = userId;
        this.username = username;
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
}
