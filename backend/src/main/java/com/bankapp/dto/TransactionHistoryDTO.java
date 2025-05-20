package com.bankapp.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;

public class TransactionHistoryDTO {
    private String transactionType; // "TRANSFER", "DEPOSIT", "WITHDRAW"
    private String fromAccountIban;
    private String fromAccountHolderName;
    private String toAccountIban;
    private String toAccountHolderName;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private String description;

    // Default constructor
    public TransactionHistoryDTO() {}

    // Constructor from Transaction
    public TransactionHistoryDTO(Transaction transaction) {
        try {
            // Handle null transactionType for existing transactions
            if (transaction.getTransactionType() == null) {
                // For legacy transactions, determine type based on accounts
                if (transaction.getFromAccount() != null && transaction.getToAccount() != null) {
                    this.transactionType = "TRANSFER";
                } else if (transaction.getToAccount() != null) {
                    this.transactionType = "DEPOSIT";
                } else if (transaction.getFromAccount() != null) {
                    this.transactionType = "WITHDRAW";
                } else {
                    this.transactionType = "UNKNOWN";
                }
            } else {
                this.transactionType = transaction.getTransactionType().toString();
            }
        } catch (Exception e) {
            // Safety mechanism for any unexpected issues
            this.transactionType = "TRANSFER";
        }
        
        try {
            if (transaction.getFromAccount() != null) {
                this.fromAccountIban = getAccountIban(transaction.getFromAccount());
                this.fromAccountHolderName = transaction.getFromAccount().getUser().getName();
            }
        } catch (Exception e) {
            // In case of any issue with from account
        }
        
        try {
            if (transaction.getToAccount() != null) {
                this.toAccountIban = getAccountIban(transaction.getToAccount());
                this.toAccountHolderName = transaction.getToAccount().getUser().getName();
            }
        } catch (Exception e) {
            // In case of any issue with to account
        }
        
        this.amount = transaction.getAmount();
        this.timestamp = transaction.getTimestamp();
        this.description = transaction.getDescription();
    }

    // Constructor from AtmOperation
    public TransactionHistoryDTO(AtmOperation atmOperation) {
        try {
            this.transactionType = atmOperation.getOperationType().toString();
            
            if (atmOperation.getAccount() != null) {
                if (atmOperation.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                    this.toAccountIban = getAccountIban(atmOperation.getAccount());
                    try {
                        this.toAccountHolderName = atmOperation.getAccount().getUser().getName();
                    } catch (Exception e) {
                        this.toAccountHolderName = "Account Owner";
                    }
                } else {
                    this.fromAccountIban = getAccountIban(atmOperation.getAccount());
                    try {
                        this.fromAccountHolderName = atmOperation.getAccount().getUser().getName();
                    } catch (Exception e) {
                        this.fromAccountHolderName = "Account Owner";
                    }
                }
            }
            
            this.amount = atmOperation.getAmount();
            this.timestamp = atmOperation.getTimestamp();
            this.description = "ATM " + atmOperation.getOperationType().toString();
        } catch (Exception e) {
            // In case of any issues
            this.transactionType = "ATM";
            this.amount = atmOperation.getAmount();
            this.timestamp = atmOperation.getTimestamp();
            this.description = "ATM Operation";
        }
    }

    // Get IBAN from account, or generate one if not available
    private static String getAccountIban(Account account) {
        if (account == null) {
            return null;
        }
        
        // Use the account's IBAN if available
        String iban = account.getIban();
        if (iban != null && !iban.isEmpty()) {
            return iban;
        }
        
        // Fall back to generating an IBAN from ID if needed
        return account.getIban();
    }

    // Getters and setters
    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getFromAccountIban() {
        return fromAccountIban;
    }

    public void setFromAccountIban(String fromAccountIban) {
        this.fromAccountIban = fromAccountIban;
    }

    public String getFromAccountHolderName() {
        return fromAccountHolderName;
    }

    public void setFromAccountHolderName(String fromAccountHolderName) {
        this.fromAccountHolderName = fromAccountHolderName;
    }

    public String getToAccountIban() {
        return toAccountIban;
    }

    public void setToAccountIban(String toAccountIban) {
        this.toAccountIban = toAccountIban;
    }

    public String getToAccountHolderName() {
        return toAccountHolderName;
    }

    public void setToAccountHolderName(String toAccountHolderName) {
        this.toAccountHolderName = toAccountHolderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}