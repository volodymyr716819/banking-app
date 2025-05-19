package com.bankapp.dto;

import java.math.BigDecimal;

import com.bankapp.model.Account;

public class AccountDTO {
    public Long id;
    public String type;
    public BigDecimal balance;
    public BigDecimal dailyLimit;
    public BigDecimal absoluteLimit;
    public Long userId;

    public AccountDTO(Account account) {
        this.id = account.getId();
        this.type = account.getType();
        this.balance = account.getBalance();
        this.dailyLimit = account.getDailyLimit();
        this.absoluteLimit = account.getAbsoluteLimit();
        this.userId = account.getUser().getId();
    }
}
