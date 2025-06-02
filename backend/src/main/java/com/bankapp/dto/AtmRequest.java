package com.bankapp.dto;

import java.math.BigDecimal;

public class AtmRequest {

    private Long accountId;
    private BigDecimal amount;
    private String pin; // Added PIN for verification

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getPin() {
        return pin;
    }
    
    public void setPin(String pin) {
        this.pin = pin;
    }
}
