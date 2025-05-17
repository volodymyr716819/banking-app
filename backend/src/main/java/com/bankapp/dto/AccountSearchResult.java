package com.bankapp.dto;

public class AccountSearchResult {
    private Long accountId;
    private String accountType;
    private String iban;
    private Long userId;
    private String userName;
    private String userEmail;

    public AccountSearchResult() {
    }

    public AccountSearchResult(Long accountId, String accountType, String iban, Long userId, String userName, String userEmail) {
        this.accountId = accountId;
        this.accountType = accountType;
        this.iban = iban;
        this.userId = userId;
        this.userName = userName;
        this.userEmail = userEmail;
    }

    // Getters and setters
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }
}