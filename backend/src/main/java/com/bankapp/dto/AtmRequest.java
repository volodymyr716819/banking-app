package com.bankapp.dto;
import com.bankapp.model.AtmOperation.OperationType;

import java.math.BigDecimal;

public class AtmRequest {

    private Long accountId;
    private BigDecimal amount;
    private char[] pin;
    private OperationType operationType;

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
    
    public char[] getPin() {
        return pin;
    }

    public void setPin(char[] pin) {
        this.pin = pin;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
}
