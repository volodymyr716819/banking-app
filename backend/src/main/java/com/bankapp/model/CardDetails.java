package com.bankapp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_details")
public class CardDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    // Store PIN securely using hashing
    @Column(nullable = false)
    private String hashedPin;
    
    // Flag to indicate if this is the first use of the card
    @Column(nullable = false)
    private boolean pinCreated = false;
    
    // Track PIN change history
    private LocalDateTime lastPinChanged;

    // Constructors
    public CardDetails() {
    }

    public CardDetails(Account account, String hashedPin) {
        this.account = account;
        this.hashedPin = hashedPin;
        this.pinCreated = true;
        this.lastPinChanged = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getHashedPin() {
        return hashedPin;
    }

    public void setHashedPin(String hashedPin) {
        this.hashedPin = hashedPin;
        this.lastPinChanged = LocalDateTime.now();
    }

    public boolean isPinCreated() {
        return pinCreated;
    }

    public void setPinCreated(boolean pinCreated) {
        this.pinCreated = pinCreated;
    }

    public LocalDateTime getLastPinChanged() {
        return lastPinChanged;
    }

    public void setLastPinChanged(LocalDateTime lastPinChanged) {
        this.lastPinChanged = lastPinChanged;
    }
}