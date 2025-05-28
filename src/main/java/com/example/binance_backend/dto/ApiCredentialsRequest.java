// src/main/java/com/example/binance_backend/dto/ApiCredentialsRequest.java
package com.example.binance_backend.dto;

public class ApiCredentialsRequest {
    private String userId;
    private String apiKey;
    private String secretKey;

    public ApiCredentialsRequest() {}

    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiKey() {
        return apiKey;
    }
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getSecretKey() {
        return secretKey;
    }
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }
}
