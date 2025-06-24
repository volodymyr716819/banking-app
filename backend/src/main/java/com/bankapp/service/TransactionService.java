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

    /** Get transaction history */
    public List<TransactionHistoryDTO> getTransactionHistory(TransactionFilterRequest filters, User currentUser) {
        List<TransactionHistoryDTO> results;
        boolean isEmployee = currentUser.getRole().equalsIgnoreCase("EMPLOYEE");
        
        // Check if employee is filtering by specific user
        if (isEmployee && filters.getUserId() != null) {
            results = getUserTransactions(filters.getUserId());
        }
        // Employee with no filters - show all
        else if (isEmployee) {
            results = getAllTransactions();
        }
        // Customer - show only their own
        else {
            results = getUserTransactions(currentUser.getId());
        }
        
        return applyFilters(results, filters);
    }
    
    private List<TransactionHistoryDTO> getAllTransactions() {
        List<TransactionHistoryDTO> results = new ArrayList<>();
        results.addAll(convertTransactions(transactionRepository.findAll()));
        results.addAll(convertAtmOperations(atmOperationRepository.findAll()));
        return results;
    }
    
    private List<TransactionHistoryDTO> getUserTransactions(Long userId) {
        List<TransactionHistoryDTO> results = new ArrayList<>();
        results.addAll(convertTransactions(
            transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId)
        ));
        results.addAll(convertAtmOperations(
            atmOperationRepository.findByAccount_User_Id(userId)
        ));
        return results;
    }
    
    private List<TransactionHistoryDTO> convertTransactions(List<Transaction> transactions) {
        return transactions.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    private List<TransactionHistoryDTO> convertAtmOperations(List<AtmOperation> operations) {
        return operations.stream().map(this::convertToDto).collect(Collectors.toList());
    }
    
    private List<TransactionHistoryDTO> applyFilters(List<TransactionHistoryDTO> transactions, TransactionFilterRequest filters) {
        List<TransactionHistoryDTO> filtered = applyFilters(
            transactions, 
            filters.getStartDate(), 
            filters.getEndDate(), 
            filters.getMinAmount(), 
            filters.getMaxAmount()
        );
        
        // Sort by date (newest first)
        filtered.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        return filtered;
    }

    // Convert any transaction to DTO
    private TransactionHistoryDTO convertToDto(Object obj) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        
        if (obj instanceof Transaction) {
            Transaction t = (Transaction) obj;
            dto.setTransactionId(t.getId());
            
            // Set sender account information
            if (t.getFromAccount() != null) {
                Account sender = t.getFromAccount();
                dto.setSenderAccountId(sender.getId());
                dto.setFromAccountIban(sender.getIban());
                dto.setFromAccountHolderName(sender.getUser() != null ? sender.getUser().getName() : "");
            }
            
            // Set receiver account information
            if (t.getToAccount() != null) {
                Account receiver = t.getToAccount();
                dto.setReceiverAccountId(receiver.getId());
                dto.setToAccountIban(receiver.getIban());
                dto.setToAccountHolderName(receiver.getUser() != null ? receiver.getUser().getName() : "");
            }
            
            dto.setAmount(t.getAmount());
            dto.setTimestamp(t.getTimestamp());
            dto.setDescription(t.getDescription());
            dto.setTransactionType(t.getTransactionType() != null ? t.getTransactionType().toString() : "UNKNOWN");
        }
        else if (obj instanceof AtmOperation) {
            AtmOperation atm = (AtmOperation) obj;
            dto.setTransactionId(atm.getId());
            
            if (atm.getAccount() != null) {
                Account account = atm.getAccount();
                
                if (atm.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                    dto.setToAccountIban(account.getIban());
                    dto.setToAccountHolderName(account.getUser() != null ? account.getUser().getName() : "");
                    dto.setReceiverAccountId(account.getId());
                } else if (atm.getOperationType() == AtmOperation.OperationType.WITHDRAW) {
                    dto.setFromAccountIban(account.getIban());
                    dto.setFromAccountHolderName(account.getUser() != null ? account.getUser().getName() : "");
                    dto.setSenderAccountId(account.getId());
                }
            }
            
            dto.setAmount(atm.getAmount());
            dto.setTimestamp(atm.getTimestamp());
            dto.setDescription("ATM " + atm.getOperationType());
            dto.setTransactionType(atm.getOperationType().toString());
        }
        
        return dto;
    }
    
    // Apply date and amount filters to transactions
    private List<TransactionHistoryDTO> applyFilters(
            List<TransactionHistoryDTO> transactions,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        
        // Parse dates
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(startDateStr, DATE_FORMATTER);
                startDate = date.atStartOfDay();
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid start date format. Use yyyy-MM-dd");
            }
        }
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(endDateStr, DATE_FORMATTER);
                endDate = date.atTime(LocalTime.MAX);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid end date format. Use yyyy-MM-dd");
            }
        }
        
        // Create final variables for lambda
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;
        
        // Apply filters
        return transactions.stream()
            .filter(tx -> {
                if (finalStartDate != null && tx.getTimestamp().isBefore(finalStartDate)) return false;
                if (finalEndDate != null && tx.getTimestamp().isAfter(finalEndDate)) return false;
                if (minAmount != null && tx.getAmount().compareTo(minAmount) < 0) return false;
                if (maxAmount != null && tx.getAmount().compareTo(maxAmount) > 0) return false;
                return true;
            })
            .collect(Collectors.toList());
    }
    
    // Entry point for transfers (decides between IBAN or account ID logic)
    @Transactional
    public void processTransfer(TransferRequest request) {
        if (request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
           throw new IllegalArgumentException("Amount must be greater than zero");
        }

        boolean usingIban = request.getSenderIban() != null && !request.getSenderIban().isEmpty() &&
                         request.getReceiverIban() != null && !request.getReceiverIban().isEmpty();

        // If both IBANs are used, delegate to IBAN-based transfer method
        if (usingIban) {
        transferMoneyByIban(
            request.getSenderIban(),
            request.getReceiverIban(),
            request.getAmount(),
            request.getDescription()
        );
        // else fallback to ID-based transfer
        } else {
            transferMoney(request.getSenderAccountId(), request.getReceiverAccountId(), request.getAmount(), request.getDescription());
        }
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

        // Delegate to ID-based logic
        transferMoney(senderOpt.get().getId(), receiverOpt.get().getId(), amount, description);
    }
    
    // Handles transferring money from one account to another (by ID)
    @Transactional
    public void transferMoney(Long senderAccountId, Long receiverAccountId, BigDecimal amount, String description) {
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