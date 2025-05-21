package com.bankapp.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.util.IbanGenerator;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AtmOperationRepository atmOperationRepository;

    /**
     * Get transaction history for a specific user, including regular transfers and ATM operations
     */
    public List<TransactionHistoryDTO> getUserTransactionHistory(Long userId) {
        // Get regular transactions
        List<Transaction> transactions = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
        
        // Get ATM operations
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_User_Id(userId);
        
        // Convert regular transactions to DTOs
        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(TransactionHistoryDTO::new)
                .collect(Collectors.toList());
        
        // Convert ATM operations to DTOs and add them to the result
        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(TransactionHistoryDTO::new)
                .collect(Collectors.toList());
        
        // Combine both lists
        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);
        
        // Sort by timestamp (newest first)
        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        return combinedHistory;
    }
    
    /**
     * Get transaction history for a specific account, including regular transfers and ATM operations
     */
    public List<TransactionHistoryDTO> getAccountTransactionHistory(Long accountId) {
        // Get regular transactions for this account (both sent and received)
        List<Transaction> transactions = transactionRepository.findByFromAccount_IdOrToAccount_Id(accountId, accountId);
        
        // Get ATM operations for this account
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_Id(accountId);
        
        // Convert regular transactions to DTOs
        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(TransactionHistoryDTO::new)
                .collect(Collectors.toList());
        
        // Convert ATM operations to DTOs and add them to the result
        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(TransactionHistoryDTO::new)
                .collect(Collectors.toList());
        
        // Combine both lists
        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);
        
        // Sort by timestamp (newest first)
        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        return combinedHistory;
    }
    
    /**
     * Get transaction history for a specific account identified by IBAN
     */
    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(String iban) {
        // Find account by IBAN
        Optional<Account> accountOpt = accountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for IBAN: " + iban);
        }
        
        // Get the account ID and use the existing method
        return getAccountTransactionHistory(accountOpt.get().getId());
    }
    
    /**
     * Legacy method for backward compatibility
     */
    public List<Transaction> getAccountHistory(Long userId) {
        return transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
    }

    @Transactional
    public void transferMoney(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String description) {
        // Validate amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Find accounts
        Optional<Account> senderOpt = accountRepository.findById(senderAccountId);
        Optional<Account> receiverOpt = accountRepository.findById(receiverAccountId);

        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender or receiver account not found");
        }

        Account sender = senderOpt.get();
        Account receiver = receiverOpt.get();
        
        // Check if both accounts are approved
        if (!sender.isApproved()) {
            throw new IllegalArgumentException("Sender account is not approved for transactions");
        }
        
        if (!receiver.isApproved()) {
            throw new IllegalArgumentException("Receiver account is not approved for transactions");
        }
        
        // Check if accounts are closed
        if (sender.isClosed()) {
            throw new IllegalArgumentException("Sender account is closed and cannot make transactions");
        }
        
        if (receiver.isClosed()) {
            throw new IllegalArgumentException("Receiver account is closed and cannot receive transactions");
        }

        // Check if sender has sufficient balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Sender has insufficient balance");
        }

        // Process transaction
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Create transaction record
        Transaction transaction = new Transaction();
        transaction.setFromAccount(sender); 
        transaction.setToAccount(receiver); 
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transactionRepository.save(transaction);
    }
    
    /**
     * Transfer money using IBAN instead of account IDs
     */
    @Transactional
    public void transferMoneyByIban(String senderIban, String receiverIban, BigDecimal amount, String description) {
        // Validate IBANs
        if (!IbanGenerator.validateIban(senderIban)) {
            throw new IllegalArgumentException("Invalid sender IBAN");
        }
        
        if (!IbanGenerator.validateIban(receiverIban)) {
            throw new IllegalArgumentException("Invalid receiver IBAN");
        }
        
        // Find accounts by IBAN
        Optional<Account> senderOpt = accountRepository.findByIban(senderIban);
        Optional<Account> receiverOpt = accountRepository.findByIban(receiverIban);
        
        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender account not found");
        }
        
        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver account not found");
        }
        
        // Use the existing method with the found account IDs
        transferMoney(senderOpt.get().getId(), receiverOpt.get().getId(), amount, description);
    }
}