package com.bankapp.service;

import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
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

    public List<Transaction> getAccountHistory(Long accountId) {
        return transactionRepository.findBySenderAccountIdOrReceiverAccountId(accountId, accountId);
    }
    
    /**
     * Get all transactions for a user, optionally filtered by account type
     * @param userId The user ID
     * @param accountType Optional filter for account type (CHECKING or SAVINGS)
     * @return List of transactions
     */
    public List<Transaction> getUserTransactions(Long userId, String accountType) {
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
        
        // Sort by timestamp descending (most recent first)
        allTransactions.sort((t1, t2) -> t2.getTimestamp().compareTo(t1.getTimestamp()));
        
        return allTransactions;
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
}