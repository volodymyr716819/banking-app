package com.bankapp.dto;

import java.math.BigDecimal;

public class AccountDTO {
    private Long id;
    private String type;
    private BigDecimal balance;
    private BigDecimal dailyLimit;
    private BigDecimal absoluteLimit;
    private Long userId;
    private String iban;
    private String formattedIban;
    private String ownerName;
    private boolean approved;
    private boolean closed;


    public AccountDTO() {
    }

    // optional constructor for convenience
    public AccountDTO(Long id, String type, BigDecimal balance, BigDecimal dailyLimit, BigDecimal absoluteLimit,
            Long userId, String iban, String formattedIban, String ownerName, boolean approved, boolean closed) {
        this.id = id;
        this.type = type;
        this.balance = balance;
        this.dailyLimit = dailyLimit;
        this.absoluteLimit = absoluteLimit;
        this.userId = userId;
        this.iban = iban;
        this.formattedIban = formattedIban;
        this.ownerName = ownerName;
        this.approved = approved;
        this.closed = closed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public BigDecimal getAbsoluteLimit() {
        return absoluteLimit;
    }

    public void setAbsoluteLimit(BigDecimal absoluteLimit) {
        this.absoluteLimit = absoluteLimit;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getFormattedIban() {
        return formattedIban;
    }

    public void setFormattedIban(String formattedIban) {
        this.formattedIban = formattedIban;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
}
// test for merge