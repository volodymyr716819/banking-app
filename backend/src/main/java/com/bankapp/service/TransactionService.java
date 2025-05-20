package com.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
     * Legacy method for backward compatibility
     */
    public List<Transaction> getAccountHistory(Long userId) {
        return transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
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
        transaction.setFromAccount(sender); 
        transaction.setToAccount(receiver); 
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transactionRepository.save(transaction);
    }
}
