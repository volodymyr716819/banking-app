package com.bankapp.dto;

public class PinRequest {
    private Long accountId;
    private char[] pin;
    private char[] newPin; // Used for PIN change

    // Getters and Setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public char[] getPin() { 
        return pin; 
    }

    public void setPin(char[] pin) { 
        this.pin = pin; 
    }

    public char[] getNewPin() { 
        return newPin; 
    }

    public void setNewPin(char[] newPin) { 
        this.newPin = newPin; 
    }
}