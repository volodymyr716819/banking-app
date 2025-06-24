package com.bankapp.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private String senderIban;
    private String receiverIban;
    private BigDecimal amount;
    private String description;

    // Getters and Setters
    
    public String getSenderIban() {
        return senderIban;
    }
    
    public void setSenderIban(String senderIban) {
        this.senderIban = senderIban;
    }
    
    public String getReceiverIban() {
        return receiverIban;
    }
    
    public void setReceiverIban(String receiverIban) {
        this.receiverIban = receiverIban;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
// test for merge