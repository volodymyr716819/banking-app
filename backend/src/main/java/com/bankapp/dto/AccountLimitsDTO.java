package com.bankapp.dto;

import java.math.BigDecimal;

public class AccountLimitsDTO {
    private Long accountId;
    private BigDecimal dailyTransferLimit;
    private BigDecimal minimumBalanceLimit;

    // Default constructor required for JSON deserialization
    public AccountLimitsDTO() {
    }

    public AccountLimitsDTO(Long accountId, BigDecimal dailyTransferLimit, BigDecimal minimumBalanceLimit) {
        this.accountId = accountId;
        this.dailyTransferLimit = dailyTransferLimit;
        this.minimumBalanceLimit = minimumBalanceLimit;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
}