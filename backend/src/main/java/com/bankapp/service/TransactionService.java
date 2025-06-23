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

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Main method to get transaction history based on filters and current user
     * 
     * @param filters Request containing all optional filter parameters
     * @param currentUser The authenticated user making the request
     * @return Filtered and sorted list of transactions
     */
    public List<TransactionHistoryDTO> getTransactionHistory(TransactionFilterRequest filters, User currentUser) {
        List<TransactionHistoryDTO> results = new ArrayList<>();
        
        // Check if employee - they can see all transactions
        boolean isEmployee = currentUser.getRole().equalsIgnoreCase("EMPLOYEE");
        
        // For employees with no filters, return all transactions
        if (isEmployee && filters.getUserId() == null && filters.getAccountId() == null &&
            (filters.getIban() == null || filters.getIban().isEmpty())) {
            
            // Get all transactions and ATM operations in the system
            List<Transaction> allTransactions = transactionRepository.findAll();
            List<AtmOperation> allAtmOperations = atmOperationRepository.findAll();
            
            // Convert to DTOs
            List<TransactionHistoryDTO> transactionDtos = allTransactions.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            List<TransactionHistoryDTO> atmDtos = allAtmOperations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            // Combine results
            results.addAll(transactionDtos);
            results.addAll(atmDtos);
            
        } else {
            // Validate filter has at least one main criteria for customers
            if (!isEmployee && filters.getUserId() == null && filters.getAccountId() == null && 
                (filters.getIban() == null || filters.getIban().isEmpty())) {
                // For customers, always filter by their own user ID if no filter is provided
                filters.setUserId(currentUser.getId());
            }
            
            // Permission check - customers can only see their own data
            checkPermissions(filters, currentUser, isEmployee);
            
            // Process filter by user ID
            if (filters.getUserId() != null) {
                // Get all transactions and ATM operations for this user
                List<Transaction> transactions = transactionRepository
                    .findByFromAccount_User_IdOrToAccount_User_Id(filters.getUserId(), filters.getUserId());
                
                List<AtmOperation> atmOperations = atmOperationRepository
                    .findByAccount_User_Id(filters.getUserId());
                
                // Convert to DTOs
                List<TransactionHistoryDTO> transactionDtos = transactions.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                List<TransactionHistoryDTO> atmDtos = atmOperations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                // Combine results
                results.addAll(transactionDtos);
                results.addAll(atmDtos);
            }
            
            // Process filter by account ID
            else if (filters.getAccountId() != null) {
                // Get all transactions and ATM operations for this account
                List<Transaction> transactions = transactionRepository
                    .findByFromAccount_IdOrToAccount_Id(filters.getAccountId(), filters.getAccountId());
                
                List<AtmOperation> atmOperations = atmOperationRepository
                    .findByAccount_Id(filters.getAccountId());
                
                // Convert to DTOs
                List<TransactionHistoryDTO> transactionDtos = transactions.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                List<TransactionHistoryDTO> atmDtos = atmOperations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                // Combine results
                results.addAll(transactionDtos);
                results.addAll(atmDtos);
            }
            
            // Process filter by IBAN
            else if (filters.getIban() != null && !filters.getIban().isEmpty()) {
                Optional<Account> accountOpt = accountRepository.findByIban(filters.getIban());
                
                if (accountOpt.isEmpty()) {
                    throw new IllegalArgumentException("Account not found for IBAN: " + filters.getIban());
                }
                
                Account account = accountOpt.get();
                
                // Get all transactions and ATM operations for this account
                List<Transaction> transactions = transactionRepository
                    .findByFromAccount_IdOrToAccount_Id(account.getId(), account.getId());
                
                List<AtmOperation> atmOperations = atmOperationRepository
                    .findByAccount_Id(account.getId());
                
                // Convert to DTOs
                List<TransactionHistoryDTO> transactionDtos = transactions.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                List<TransactionHistoryDTO> atmDtos = atmOperations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                
                // Combine results
                results.addAll(transactionDtos);
                results.addAll(atmDtos);
            }
        }
        
        // Apply date and amount filters
        List<TransactionHistoryDTO> filteredResults = applyFilters(
            results, 
            filters.getStartDate(), 
            filters.getEndDate(), 
            filters.getMinAmount(), 
            filters.getMaxAmount()
        );
        
        // Sort by date (newest first)
        filteredResults.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        return filteredResults;
    }
    
    /**
     * Check if the current user has permission to access the requested data
     */
    private void checkPermissions(TransactionFilterRequest filters, User currentUser, boolean isEmployee) {
        // Employees can see everything
        if (isEmployee) {
            return;
        }
        
        // For customers, check specific permissions
        
        // User ID filter - customer can only see their own data
        if (filters.getUserId() != null && !filters.getUserId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Access denied: You can only view your own transactions");
        }
        
        // Account ID filter - customer can only see their own accounts
        if (filters.getAccountId() != null) {
            Optional<Account> accountOpt = accountRepository.findById(filters.getAccountId());
            if (accountOpt.isEmpty() || !accountOpt.get().getUser().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("Access denied: Account not found or not owned by you");
            }
        }
        
        // IBAN filter - customer can only see their own accounts
        if (filters.getIban() != null && !filters.getIban().isEmpty()) {
            Optional<Account> accountOpt = accountRepository.findByIban(filters.getIban());
            if (accountOpt.isEmpty() || !accountOpt.get().getUser().getId().equals(currentUser.getId())) {
                throw new IllegalArgumentException("Access denied: Account not found or not owned by you");
            }
        }
    }

    /**
     * Convert a Transaction entity to DTO
     */
    private TransactionHistoryDTO convertToDto(Transaction t) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
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
        return dto;
    }

    /**
     * Convert an ATM operation to DTO
     */
    private TransactionHistoryDTO convertToDto(AtmOperation atm) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(atm.getId());
        
        // For ATM operations, the account is both the sender and receiver (for UI consistency)
        if (atm.getAccount() != null) {
            Account account = atm.getAccount();
            
            if (atm.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                // For deposits, the account is the receiver
                dto.setToAccountIban(account.getIban());
                dto.setToAccountHolderName(account.getUser() != null ? account.getUser().getName() : "");
                dto.setReceiverAccountId(account.getId());
            } else if (atm.getOperationType() == AtmOperation.OperationType.WITHDRAW) {
                // For withdrawals, the account is the sender
                dto.setFromAccountIban(account.getIban());
                dto.setFromAccountHolderName(account.getUser() != null ? account.getUser().getName() : "");
                dto.setSenderAccountId(account.getId());
            }
        }
        
        dto.setAmount(atm.getAmount());
        dto.setTimestamp(atm.getTimestamp());
        dto.setDescription("ATM " + atm.getOperationType());
        dto.setTransactionType(atm.getOperationType().toString());
        return dto;
    }
    
    /**
     * Apply date and amount filters to a list of transactions
     */
    private List<TransactionHistoryDTO> applyFilters(
            List<TransactionHistoryDTO> transactions,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        
        // Convert date strings to actual date objects
        LocalDateTime startDate = null;
        LocalDateTime endDate = null;
        
        if (startDateStr != null && !startDateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(startDateStr, DATE_FORMATTER);
                startDate = date.atStartOfDay(); // Use beginning of day for start date
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid start date format. Use yyyy-MM-dd");
            }
        }
        
        if (endDateStr != null && !endDateStr.isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(endDateStr, DATE_FORMATTER);
                endDate = date.atTime(LocalTime.MAX); // Use end of day for end date
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid end date format. Use yyyy-MM-dd");
            }
        }
        
        // Create final copies for lambda use
        final LocalDateTime finalStartDate = startDate;
        final LocalDateTime finalEndDate = endDate;
        final BigDecimal finalMinAmount = minAmount;
        final BigDecimal finalMaxAmount = maxAmount;
        
        // Apply all filters (date and amount)
        return transactions.stream()
            .filter(tx -> {
                // Skip transactions before start date
                if (finalStartDate != null && tx.getTimestamp().isBefore(finalStartDate)) {
                    return false;
                }
                // Skip transactions after end date
                if (finalEndDate != null && tx.getTimestamp().isAfter(finalEndDate)) {
                    return false;
                }
                
                // Skip transactions less than minimum amount
                if (finalMinAmount != null && tx.getAmount().compareTo(finalMinAmount) < 0) {
                    return false;
                }
                // Skip transactions more than maximum amount
                if (finalMaxAmount != null && tx.getAmount().compareTo(finalMaxAmount) > 0) {
                    return false;
                }
                
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