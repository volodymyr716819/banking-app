package com.bankapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionHistoryDTO {

    private Long transactionId;
    private String transactionType; // TRANSFER, DEPOSIT, WITHDRAW
    private Long senderAccountId;
    private Long receiverAccountId;
    private String fromAccountIban;
    private String fromAccountHolderName;
    private String toAccountIban;
    private String toAccountHolderName;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;
    private String type; 

    public TransactionHistoryDTO() {}

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

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

    public String getFromAccountIban() {
        return fromAccountIban;
    }

    public void setFromAccountIban(String fromAccountIban) {
        this.fromAccountIban = fromAccountIban;
    }

    public String getFromAccountHolderName() {
        return fromAccountHolderName;
    }

    public void setFromAccountHolderName(String fromAccountHolderName) {
        this.fromAccountHolderName = fromAccountHolderName;
    }

    public String getToAccountIban() {
        return toAccountIban;
    }

    public void setToAccountIban(String toAccountIban) {
        this.toAccountIban = toAccountIban;
    }

    public String getToAccountHolderName() {
        return toAccountHolderName;
    }

    public void setToAccountHolderName(String toAccountHolderName) {
        this.toAccountHolderName = toAccountHolderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
// test for merge