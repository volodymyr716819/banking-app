package com.bankapp.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.bankapp.util.IbanGenerator;
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

    @Column(nullable = false)
    private BigDecimal dailyLimit = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal absoluteLimit = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean closed = false;
    
    @Column(unique = true)
    private String iban;

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

    public BigDecimal getAbsoluteLimit() {
        return absoluteLimit;
    }

    public void setAbsoluteLimit(BigDecimal absoluteLimit) {
        this.absoluteLimit = absoluteLimit;
    }

    public BigDecimal getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(BigDecimal dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public boolean isClosed() {
        return closed;
    }
    
    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public String getIban() {
        // If IBAN is not set, generate it
        if (iban == null || iban.isEmpty()) {
            if (id != null) {
                iban = IbanGenerator.generateIban(id);
            }
        }
        return iban;
    }
    
    public void setIban(String iban) {
        this.iban = iban;
    }
    
    /**
     * Returns the IBAN with proper formatting for display
     * @return IBAN with spaces for better readability
     */
    @Transient
    public String getFormattedIban() {
        return IbanGenerator.formatIban(getIban());
    }
    
    /**
     * Called after the entity is persisted to generate the IBAN based on the ID.
     */
    @PostPersist
    public void generateIban() {
        if (id != null) {
            iban = IbanGenerator.generateIban(id);
        }
    }
}
// test for merge