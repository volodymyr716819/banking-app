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

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.dto.TransferRequest;
import com.bankapp.dto.PinRequest;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.util.IbanGenerator;
import com.bankapp.exception.UnapprovedAccountException;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Converts a Transaction entity into a DTO for frontend use
    private TransactionHistoryDTO mapTransactionToDTO(Transaction t) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(t.getId());
        dto.setSenderAccountId(t.getFromAccount() != null ? t.getFromAccount().getId() : null);
        dto.setReceiverAccountId(t.getToAccount() != null ? t.getToAccount().getId() : null);
        dto.setAmount(t.getAmount());
        dto.setTimestamp(t.getTimestamp());
        dto.setDescription(t.getDescription());
        dto.setTransactionType(t.getTransactionType() != null ? t.getTransactionType().toString() : "UNKNOWN");
        return dto;
    }

    // Converts an ATM operation (withdraw/deposit) into a DTO
    private TransactionHistoryDTO mapAtmOperationToDTO(AtmOperation atm) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(atm.getId());
        dto.setAmount(atm.getAmount());
        dto.setTimestamp(atm.getTimestamp());
        dto.setDescription("ATM " + atm.getOperationType());
        dto.setTransactionType(atm.getOperationType().toString());
        return dto;
    }

    // Get user transaction history without filters
    public List<TransactionHistoryDTO> getUserTransactionHistory(Long userId) {
        return getUserTransactionHistory(userId, null, null, null, null);
    }
    
    // Get user transaction history with optional date and amount filters
    public List<TransactionHistoryDTO> getUserTransactionHistory(
            Long userId, 
            String startDateStr, 
            String endDateStr, 
            BigDecimal minAmount, 
            BigDecimal maxAmount) {
        
        // Get regular bank transfers
        List<Transaction> transactions = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
        
        // Get ATM operations (deposits/withdrawals)
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_User_Id(userId);

        // Convert to DTOs for frontend
        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(this::mapTransactionToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(this::mapAtmOperationToDTO)
                .collect(Collectors.toList());

        // Combine all transactions
        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);

        // Sort by date (newest first)
        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        // Apply date and amount filters
        return filterTransactions(combinedHistory, startDateStr, endDateStr, minAmount, maxAmount);
    }

    // Get account transaction history without filters
    public List<TransactionHistoryDTO> getAccountTransactionHistory(Long accountId) {
        return getAccountTransactionHistory(accountId, null, null, null, null);
    }
    
    // Get account transaction history with optional date and amount filters
    public List<TransactionHistoryDTO> getAccountTransactionHistory(
            Long accountId,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
            
        // Get regular bank transfers for this account
        List<Transaction> transactions = transactionRepository.findByFromAccount_IdOrToAccount_Id(accountId, accountId);
        
        // Get ATM operations for this account
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_Id(accountId);

        // Convert to DTOs for frontend
        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(this::mapTransactionToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(this::mapAtmOperationToDTO)
                .collect(Collectors.toList());

        // Combine both transaction types
        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);

        // Sort by date (newest first)
        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        // Apply date and amount filters
        return filterTransactions(combinedHistory, startDateStr, endDateStr, minAmount, maxAmount);
    }

    // Get account history by IBAN without filters
    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(String iban) {
        return getAccountTransactionHistoryByIban(iban, null, null, null, null);
    }
    
    // Get account history by IBAN with optional date and amount filters
    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(
            String iban,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        // Find account by IBAN
        Optional<Account> accountOpt = accountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for IBAN: " + iban);
        }
        // Get transaction history using account ID
        return getAccountTransactionHistory(accountOpt.get().getId(), startDateStr, endDateStr, minAmount, maxAmount);
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

    // Get account history by IBAN with authentication check
    public List<TransactionHistoryDTO> getTransactionHistoryByIbanWithAuth(String iban, User requester) {
        return getTransactionHistoryByIbanWithAuth(iban, requester, null, null, null, null);
    }
    
    // Get account history by IBAN with authentication and filters
    public List<TransactionHistoryDTO> getTransactionHistoryByIbanWithAuth(
            String iban, 
            User requester,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        // Find the account
        Account account = accountRepository.findByIban(iban)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        // Check authorization - only account owner or bank employee can view
        if (!account.getUser().getId().equals(requester.getId()) &&
           !requester.getRole().equalsIgnoreCase("EMPLOYEE")) {
           throw new IllegalArgumentException("Access denied");
        }

        // Return filtered transaction history
        return getAccountTransactionHistory(account.getId(), startDateStr, endDateStr, minAmount, maxAmount);
    }
    
    // Get user transaction history with authentication check
    public List<TransactionHistoryDTO> getTransactionsByUserWithAuth(Long userId, User requester) {
        return getTransactionsByUserWithAuth(userId, requester, null, null, null, null);
    }
    
    // Get user transaction history with authentication and filters
    public List<TransactionHistoryDTO> getTransactionsByUserWithAuth(
            Long userId, 
            User requester,
            String startDateStr,
            String endDateStr,
            BigDecimal minAmount,
            BigDecimal maxAmount) {
        // Check authorization - only the user or bank employee can view
        if (!requester.getId().equals(userId) && !requester.getRole().equalsIgnoreCase("EMPLOYEE")) {
            throw new IllegalArgumentException("Access denied");
        }

        // Return filtered transaction history
        return getUserTransactionHistory(userId, startDateStr, endDateStr, minAmount, maxAmount);
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

    // Filter transactions by date range and amount
    private List<TransactionHistoryDTO> filterTransactions(
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
        
        // Apply all filters (date and amount)
        return transactions.stream()
            .filter(tx -> {
                // Skip transactions before start date
                if (startDate != null && tx.getTimestamp().isBefore(startDate)) {
                    return false;
                }
                // Skip transactions after end date
                if (endDate != null && tx.getTimestamp().isAfter(endDate)) {
                    return false;
                }
                
                // Skip transactions less than minimum amount
                if (minAmount != null && tx.getAmount().compareTo(minAmount) < 0) {
                    return false;
                }
                // Skip transactions more than maximum amount
                if (maxAmount != null && tx.getAmount().compareTo(maxAmount) > 0) {
                    return false;
                }
                
                return true;
            })
            .collect(Collectors.toList());
    }
}
