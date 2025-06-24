package com.bankapp.dto;
import com.bankapp.model.AtmOperation.OperationType;
import com.fasterxml.jackson.annotation.JsonSetter;

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

    // Accept both string and char array for PIN
    public void setPin(char[] pin) {
        this.pin = pin;
    }
    
    @JsonSetter("pin")
    public void setPin(String pinString) {
        if (pinString != null) {
            this.pin = pinString.toCharArray();
        }
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }
    
    // Accept string for operation type and convert to enum
    @JsonSetter("operationType")
    public void setOperationType(String operationTypeString) {
        if (operationTypeString != null) {
            this.operationType = OperationType.valueOf(operationTypeString.toUpperCase());
        }
    }
}
