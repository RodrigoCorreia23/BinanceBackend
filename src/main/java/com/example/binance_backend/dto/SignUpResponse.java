// src/main/java/com/example/binance_backend/dto/SignUpResponse.java
package com.example.binance_backend.dto;

public class SignUpResponse {
    private String userId;
    private String username;

    public SignUpResponse(String userId, String username) {
        this.userId = userId;
        this.username = username;
    }

    public String getUserId() { return userId; }
    public String getUsername() { return username; }
}
