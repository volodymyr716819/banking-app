package com.bankapp.dto;

import java.math.BigDecimal;

/**
 * DTO for filtering transaction history
 * Used to simplify and standardize transaction filter parameters
 */
public class TransactionFilterRequest {
    private Long accountId;   // Filter by specific account
    private Long userId;      // For employee use only - filter by user
    private String iban;      // Filter by account IBAN
    private String startDate; // Filter by start date (format: yyyy-MM-dd)
    private String endDate;   // Filter by end date (format: yyyy-MM-dd)
    private BigDecimal minAmount; // Filter by minimum amount
    private BigDecimal maxAmount; // Filter by maximum amount

    // Default constructor
    public TransactionFilterRequest() {
    }

    // Getters and setters
    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }
}// test for merge