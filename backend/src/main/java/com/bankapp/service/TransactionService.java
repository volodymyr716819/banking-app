package com.bankapp.service;

import com.bankapp.dto.TransactionHistoryItem;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Optional;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private AtmOperationRepository atmOperationRepository;

    /**
     * Get complete transaction history for an account including transfers and ATM operations
     * 
     * @param accountId The account ID
     * @return List of TransactionHistoryItems
     */
    public List<TransactionHistoryItem> getAccountHistory(Long accountId) {
        // Get transfers
        List<Transaction> transactions = transactionRepository.findBySenderAccountIdOrReceiverAccountId(accountId, accountId);
        
        // Get ATM operations
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccountId(accountId);
        
        // Combine and convert to DTO
        return combineAndSortTransactionHistory(transactions, atmOperations);
    }
    
    /**
     * Get all transactions for a user, optionally filtered by account type
     * @param userId The user ID
     * @param accountType Optional filter for account type (CHECKING or SAVINGS)
     * @return List of TransactionHistoryItems
     */
    public List<TransactionHistoryItem> getUserTransactions(Long userId, String accountType) {
        // Get all accounts for the user
        List<Account> userAccounts = accountRepository.findByUserId(userId);
        
        if (userAccounts.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Filter accounts by type if specified
        if (accountType != null && !accountType.isEmpty()) {
            userAccounts = userAccounts.stream()
                .filter(account -> account.getType().equalsIgnoreCase(accountType))
                .collect(Collectors.toList());
        }
        
        // Get only approved accounts
        userAccounts = userAccounts.stream()
            .filter(Account::isApproved)
            .collect(Collectors.toList());
            
        if (userAccounts.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Get transactions for all accounts
        List<Transaction> allTransactions = new ArrayList<>();
        for (Account account : userAccounts) {
            allTransactions.addAll(
                transactionRepository.findBySenderAccountIdOrReceiverAccountId(
                    account.getId(), account.getId()
                )
            );
        }
        
        // Get ATM operations for this user
        List<AtmOperation> atmOperations;
        if (accountType != null && !accountType.isEmpty()) {
            atmOperations = atmOperationRepository.findByUserIdAndAccountType(userId, accountType);
        } else {
            atmOperations = atmOperationRepository.findByUserId(userId);
        }
        
        // Combine, convert to DTO, and sort
        return combineAndSortTransactionHistory(allTransactions, atmOperations);
    }

    @Transactional
    public void transferMoney(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String description) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

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

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Sender has insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = new Transaction();
        transaction.setSenderAccount(sender);
        transaction.setReceiverAccount(receiver);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transactionRepository.save(transaction);
    }
    
    /**
     * Converts a Transaction to a TransactionHistoryItem
     * 
     * @param transaction The transaction to convert
     * @return A TransactionHistoryItem
     */
    private TransactionHistoryItem convertTransactionToHistoryItem(Transaction transaction) {
        TransactionHistoryItem item = new TransactionHistoryItem();
        
        // Basic properties
        item.setId(transaction.getId());
        item.setAmount(transaction.getAmount());
        item.setTimestamp(transaction.getTimestamp());
        item.setDescription(transaction.getDescription());
        item.setType("TRANSFER");
        
        // Sender account details
        if (transaction.getSenderAccount() != null) {
            Account sender = transaction.getSenderAccount();
            item.setSenderAccountId(sender.getId());
            item.setSenderAccountType(sender.getType());
            item.setSenderIban(sender.getIban());
        }
        
        // Receiver account details
        if (transaction.getReceiverAccount() != null) {
            Account receiver = transaction.getReceiverAccount();
            item.setReceiverAccountId(receiver.getId());
            item.setReceiverAccountType(receiver.getType());
            item.setReceiverIban(receiver.getIban());
        }
        
        return item;
    }
    
    /**
     * Converts an ATM Operation to a TransactionHistoryItem
     * 
     * @param operation The ATM operation to convert
     * @return A TransactionHistoryItem
     */
    private TransactionHistoryItem convertAtmOperationToHistoryItem(AtmOperation operation) {
        TransactionHistoryItem item = new TransactionHistoryItem();
        
        // Basic properties
        item.setId(operation.getId());
        item.setAmount(operation.getAmount());
        item.setTimestamp(operation.getTimestamp());
        item.setType(operation.getOperationType().toString());
        
        // Set appropriate description based on operation type
        if (operation.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
            item.setDescription("ATM Deposit");
        } else {
            item.setDescription("ATM Withdrawal");
        }
        
        // Account details
        if (operation.getAccount() != null) {
            Account account = operation.getAccount();
            item.setAccountId(account.getId());
            item.setAccountType(account.getType());
            item.setAccountIban(account.getIban());
            
            // For consistency in the UI, also set either sender or receiver based on operation type
            if (operation.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                // For deposits, the account is the receiver
                item.setReceiverAccountId(account.getId());
                item.setReceiverAccountType(account.getType());
                item.setReceiverIban(account.getIban());
                item.setSenderAccountId(null);
                item.setSenderAccountType("External");
                item.setSenderIban("CASH");
            } else {
                // For withdrawals, the account is the sender
                item.setSenderAccountId(account.getId());
                item.setSenderAccountType(account.getType());
                item.setSenderIban(account.getIban());
                item.setReceiverAccountId(null);
                item.setReceiverAccountType("External");
                item.setReceiverIban("CASH");
            }
        }
        
        return item;
    }
    
    /**
     * Combines, converts, and sorts transactions and ATM operations into a unified history
     * 
     * @param transactions List of transactions
     * @param atmOperations List of ATM operations
     * @return Combined and sorted list of TransactionHistoryItems
     */
    private List<TransactionHistoryItem> combineAndSortTransactionHistory(
            List<Transaction> transactions, List<AtmOperation> atmOperations) {
        
        List<TransactionHistoryItem> historyItems = new ArrayList<>();
        
        // Convert transactions
        if (transactions != null) {
            for (Transaction transaction : transactions) {
                historyItems.add(convertTransactionToHistoryItem(transaction));
            }
        }
        
        // Convert ATM operations
        if (atmOperations != null) {
            for (AtmOperation operation : atmOperations) {
                historyItems.add(convertAtmOperationToHistoryItem(operation));
            }
        }
        
        // Sort by timestamp descending (most recent first)
        historyItems.sort(Comparator.comparing(TransactionHistoryItem::getTimestamp).reversed());
        
        return historyItems;
    }
}