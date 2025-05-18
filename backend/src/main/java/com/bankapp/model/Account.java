package com.bankapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonBackReference;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JsonBackReference
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String type; // CHECKING or SAVINGS
    private BigDecimal balance;
    private boolean approved = false;
    private LocalDateTime createdAt = LocalDateTime.now();
    
    // Default daily transfer limit: 10,000
    private BigDecimal dailyTransferLimit = new BigDecimal("10000.00");
    
    // Default minimum balance limit: 0
    private BigDecimal minimumBalanceLimit = BigDecimal.ZERO;
    
    // Track daily transfers for limit checking
    @Transient
    private BigDecimal dailyTransfersTotal = BigDecimal.ZERO;
    
    @Transient
    private LocalDateTime lastTransferDate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public BigDecimal getDailyTransferLimit() {
        return dailyTransferLimit;
    }
    
    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) {
        this.dailyTransferLimit = dailyTransferLimit;
    }
    
    public BigDecimal getMinimumBalanceLimit() {
        return minimumBalanceLimit;
    }
    
    public void setMinimumBalanceLimit(BigDecimal minimumBalanceLimit) {
        this.minimumBalanceLimit = minimumBalanceLimit;
    }
    
    public BigDecimal getDailyTransfersTotal() {
        return dailyTransfersTotal;
    }
    
    public void setDailyTransfersTotal(BigDecimal dailyTransfersTotal) {
        this.dailyTransfersTotal = dailyTransfersTotal;
    }
    
    public LocalDateTime getLastTransferDate() {
        return lastTransferDate;
    }
    
    public void setLastTransferDate(LocalDateTime lastTransferDate) {
        this.lastTransferDate = lastTransferDate;
    }
    
    /**
     * Generates a pseudo-IBAN for this account.
     * Format: "XY00BANK" + padded account ID
     * XY - Country code (using XX for our demo bank)
     * 00 - Check digits (using 00 for simplicity)
     * BANK - Bank code
     * Account ID - Padded to 10 digits
     */
    @Transient
    public String getIban() {
        return "XX00BANK" + String.format("%010d", id);
    }
}
