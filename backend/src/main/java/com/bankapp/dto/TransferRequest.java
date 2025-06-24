// File: com.bankapp.dto.TransferRequest.java

package com.bankapp.dto;

import java.math.BigDecimal;
import java.util.Arrays;

public class TransferRequest {

    private char[] senderIban;
    private char[] receiverIban;
    private BigDecimal amount;
    private String description;

    public char[] getSenderIban() {
        return senderIban;
    }

    public void setSenderIban(char[] senderIban) {
        this.senderIban = senderIban;
    }

    public char[] getReceiverIban() {
        return receiverIban;
    }

    public void setReceiverIban(char[] receiverIban) {
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

    // Clear sensitive data after use
    public void clearIban() {
        if (senderIban != null) Arrays.fill(senderIban, '0');
        if (receiverIban != null) Arrays.fill(receiverIban, '0');
    }
}
