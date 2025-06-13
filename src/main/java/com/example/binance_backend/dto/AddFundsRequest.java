package com.example.binance_backend.dto;

import java.math.BigDecimal;

public class AddFundsRequest {
    private BigDecimal amount;

    public AddFundsRequest() {}

    public AddFundsRequest(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getAmount() {
        return amount;
    }
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
