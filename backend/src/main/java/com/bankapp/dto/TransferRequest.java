package com.bankapp.dto;

import java.math.BigDecimal;

public class TransferRequest {

    private Long senderAccountId;
    private Long receiverAccountId;
    private String senderIban;
    private String receiverIban;
    private BigDecimal amount;
    private String description;

    // Getters and Setters
    public Long getSenderAccountId() {
        return senderAccountId;
    }

    public void setSenderAccountId(Long senderAccountId) {
        this.senderAccountId = senderAccountId;
    }

    public Long getReceiverAccountId() {
        return receiverAccountId;
    }

    public void setReceiverAccountId(Long receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }
    
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
