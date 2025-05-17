package com.bankapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO to represent either a Transaction or ATM Operation in a unified transaction history
 */
public class TransactionHistoryItem {
    
    private Long id;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;
    private String type; // "TRANSFER", "DEPOSIT", or "WITHDRAW"
    
    // For transfers
    private Long senderAccountId;
    private String senderAccountType;
    private String senderIban;
    
    private Long receiverAccountId;
    private String receiverAccountType;
    private String receiverIban;
    
    // For ATM operations
    private Long accountId;
    private String accountType;
    private String accountIban;
    
    // Default constructor
    public TransactionHistoryItem() {}
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public Long getSenderAccountId() {
        return senderAccountId;
    }
    
    public void setSenderAccountId(Long senderAccountId) {
        this.senderAccountId = senderAccountId;
    }
    
    public String getSenderAccountType() {
        return senderAccountType;
    }
    
    public void setSenderAccountType(String senderAccountType) {
        this.senderAccountType = senderAccountType;
    }
    
    public String getSenderIban() {
        return senderIban;
    }
    
    public void setSenderIban(String senderIban) {
        this.senderIban = senderIban;
    }
    
    public Long getReceiverAccountId() {
        return receiverAccountId;
    }
    
    public void setReceiverAccountId(Long receiverAccountId) {
        this.receiverAccountId = receiverAccountId;
    }
    
    public String getReceiverAccountType() {
        return receiverAccountType;
    }
    
    public void setReceiverAccountType(String receiverAccountType) {
        this.receiverAccountType = receiverAccountType;
    }
    
    public String getReceiverIban() {
        return receiverIban;
    }
    
    public void setReceiverIban(String receiverIban) {
        this.receiverIban = receiverIban;
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public String getAccountIban() {
        return accountIban;
    }
    
    public void setAccountIban(String accountIban) {
        this.accountIban = accountIban;
    }
}