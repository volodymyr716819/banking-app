package com.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.dto.TransactionFilterRequest;
import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.exception.UnapprovedAccountException;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.util.IbanGenerator;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AtmOperationRepository atmOperationRepository;
    
    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        AtmOperationRepository atmOperationRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.atmOperationRepository = atmOperationRepository;
    }
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /** Get transaction history with role-based access control and filtering */
    public List<TransactionHistoryDTO> getTransactionHistory(TransactionFilterRequest filters, User currentUser) {
        boolean isEmployee = currentUser.getRole().equalsIgnoreCase("EMPLOYEE");
        Long targetUserId = determineTargetUserId(filters, currentUser, isEmployee);
        
        List<TransactionHistoryDTO> transactions = fetchTransactionHistory(targetUserId);
        return applyFiltersAndSort(transactions, filters);
    }
    
    /** Determine which user's transactions to fetch based on role and filters */
    private Long determineTargetUserId(TransactionFilterRequest filters, User currentUser, boolean isEmployee) {
        if (isEmployee && filters.getUserId() != null) {
            return filters.getUserId(); // Employee filtering by specific user
        }
        if (isEmployee) {
            return null; // Employee wants all transactions
        }
        return currentUser.getId(); // Customer sees only their own
    }
    
    /** Fetch transaction history for specific user or all users */
    private List<TransactionHistoryDTO> fetchTransactionHistory(Long userId) {
        List<TransactionHistoryDTO> results = new ArrayList<>();
        
        // Fetch transactions and ATM operations based on user filter
        if (userId == null) {
            // Fetch all transactions for employees
            results.addAll(transactionRepository.findAll().stream()
                .map(this::convertTransactionToDto)
                .collect(Collectors.toList()));
            results.addAll(atmOperationRepository.findAll().stream()
                .map(this::convertAtmOperationToDto)
                .collect(Collectors.toList()));
        } else {
            // Fetch user-specific transactions
            results.addAll(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId).stream()
                .map(this::convertTransactionToDto)
                .collect(Collectors.toList()));
            results.addAll(atmOperationRepository.findByAccount_User_Id(userId).stream()
                .map(this::convertAtmOperationToDto)
                .collect(Collectors.toList()));
        }
        
        return results;
    }
    
    /** Apply filters and sort transactions by timestamp (newest first) */
    private List<TransactionHistoryDTO> applyFiltersAndSort(List<TransactionHistoryDTO> transactions, TransactionFilterRequest filters) {
        LocalDateTime startDate = parseDateString(filters.getStartDate(), true);
        LocalDateTime endDate = parseDateString(filters.getEndDate(), false);
        
        return transactions.stream()
            .filter(tx -> isWithinDateRange(tx.getTimestamp(), startDate, endDate))
            .filter(tx -> isWithinAmountRange(tx.getAmount(), filters.getMinAmount(), filters.getMaxAmount()))
            .sorted(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    /** Convert Transaction entity to DTO */
    private TransactionHistoryDTO convertTransactionToDto(Transaction transaction) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(transaction.getId());
        dto.setAmount(transaction.getAmount());
        dto.setTimestamp(transaction.getTimestamp());
        dto.setDescription(transaction.getDescription());
        dto.setTransactionType(transaction.getTransactionType() != null ? transaction.getTransactionType().toString() : "UNKNOWN");
        
        // Set sender account details
        if (transaction.getFromAccount() != null) {
            Account sender = transaction.getFromAccount();
            dto.setSenderAccountId(sender.getId());
            dto.setFromAccountIban(sender.getIban());
            dto.setFromAccountHolderName(sender.getUser() != null ? sender.getUser().getName() : "");
        }
        
        // Set receiver account details
        if (transaction.getToAccount() != null) {
            Account receiver = transaction.getToAccount();
            dto.setReceiverAccountId(receiver.getId());
            dto.setToAccountIban(receiver.getIban());
            dto.setToAccountHolderName(receiver.getUser() != null ? receiver.getUser().getName() : "");
        }
        
        return dto;
    }
    
    /** Convert ATM Operation entity to DTO */
    private TransactionHistoryDTO convertAtmOperationToDto(AtmOperation atmOperation) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(atmOperation.getId());
        dto.setAmount(atmOperation.getAmount());
        dto.setTimestamp(atmOperation.getTimestamp());
        dto.setDescription("ATM " + atmOperation.getOperationType());
        dto.setTransactionType(atmOperation.getOperationType().toString());
        
        if (atmOperation.getAccount() != null) {
            Account account = atmOperation.getAccount();
            String accountHolderName = account.getUser() != null ? account.getUser().getName() : "";
            
            if (atmOperation.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                dto.setReceiverAccountId(account.getId());
                dto.setToAccountIban(account.getIban());
                dto.setToAccountHolderName(accountHolderName);
            } else if (atmOperation.getOperationType() == AtmOperation.OperationType.WITHDRAW) {
                dto.setSenderAccountId(account.getId());
                dto.setFromAccountIban(account.getIban());
                dto.setFromAccountHolderName(accountHolderName);
            }
        }
        
        return dto;
    }
    
    /** Parse date string to LocalDateTime with proper error handling */
    private LocalDateTime parseDateString(String dateStr, boolean isStartDate) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return isStartDate ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
        }
    }
    
    /** Check if timestamp is within date range */
    private boolean isWithinDateRange(LocalDateTime timestamp, LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate != null && timestamp.isBefore(startDate)) {
            return false;
        }
        if (endDate != null && timestamp.isAfter(endDate)) {
            return false;
        }
        return true;
    }
    
    /** Check if amount is within specified range */
    private boolean isWithinAmountRange(BigDecimal amount, BigDecimal minAmount, BigDecimal maxAmount) {
        if (minAmount != null && amount.compareTo(minAmount) < 0) {
            return false;
        }
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            return false;
        }
        return true;
    }
    
    // Entry point for transfers using IBAN
    @Transactional
    public void processTransfer(TransferRequest request) {
        // Validate amount
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        
        // Validate IBANs are provided
        if (request.getSenderIban() == null || request.getSenderIban().isEmpty() ||
            request.getReceiverIban() == null || request.getReceiverIban().isEmpty()) {
            throw new IllegalArgumentException("Both sender and receiver IBANs are required");
        }
        
        // Process IBAN transfer
        transferMoneyByIban(
            request.getSenderIban(),
            request.getReceiverIban(),
            request.getAmount(),
            request.getDescription()
        );
    }

    // Transfers money using IBAN values
    @Transactional
    public void transferMoneyByIban(String senderIban, String receiverIban, BigDecimal amount, String description) {
        // Validate IBAN format
        if (!IbanGenerator.validateIban(senderIban)) {
            throw new IllegalArgumentException("Invalid sender IBAN");
        }

        if (!IbanGenerator.validateIban(receiverIban)) {
            throw new IllegalArgumentException("Invalid receiver IBAN");
        }

        // Resolve accounts by IBAN
        Optional<Account> senderOpt = accountRepository.findByIban(senderIban);
        Optional<Account> receiverOpt = accountRepository.findByIban(receiverIban);

        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender account not found");
        }

        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver account not found");
        }
        
        // After getting sender and receiver accounts
        if (senderOpt.get().getId().equals(receiverOpt.get().getId())) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        // Delegate to ID-based logic
        transferMoney(senderOpt.get().getId(), receiverOpt.get().getId(), amount, description);
    }
    
    // Handles transferring money from one account to another (by ID)
    @Transactional
    public void transferMoney(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String description) {
        // Prevent self-transfer
        if (senderAccountId.equals(receiverAccountId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        
        // Must be a positive amount
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        // Validate both sender and receiver exist
        Optional<Account> senderOpt = accountRepository.findById(senderAccountId);
        Optional<Account> receiverOpt = accountRepository.findById(receiverAccountId);

        if (senderOpt.isEmpty() || receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender or receiver account not found");
        }

        Account sender = senderOpt.get();
        Account receiver = receiverOpt.get();

        // Validate both accounts are open and approved
        if (!sender.isApproved() || !receiver.isApproved()) {
            throw new IllegalArgumentException("Both accounts must be approved for transactions");
        }

        // Accounts must not be closed
        if (sender.isClosed() || receiver.isClosed()) {
            throw new IllegalArgumentException("Closed accounts cannot process transactions");
        }

        // Ensure sender has enough balance
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Sender has insufficient balance");
        }

        // Perform balance update
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        
        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Save transaction record
        Transaction transaction = new Transaction();
        transaction.setFromAccount(sender);
        transaction.setToAccount(receiver);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setTransactionType(TransactionType.TRANSFER);
        transactionRepository.save(transaction);
    }

    @Transactional
    public void depositMoney(Long accountId, BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.isApproved()) {
            throw new UnapprovedAccountException("Cannot deposit to an unapproved account");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAmount(amount);
        tx.setDescription("Deposit");
        tx.setToAccount(account);
        tx.setTransactionType(TransactionType.DEPOSIT);
        transactionRepository.save(tx);
    }
}