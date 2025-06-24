package com.bankapp.service;

import com.bankapp.dto.TransactionFilterRequest;
import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.exception.UnapprovedAccountException;
import com.bankapp.model.*;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.util.IbanGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AtmOperationRepository atmOperationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public TransactionService(
        TransactionRepository transactionRepository,
        AccountRepository accountRepository,
        AtmOperationRepository atmOperationRepository) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.atmOperationRepository = atmOperationRepository;
    }

    // Get filtered & sorted transaction + ATM history
    public List<TransactionHistoryDTO> getTransactionHistory(TransactionFilterRequest filters, User currentUser) {
        Long userId = resolveQueryUser(filters, currentUser);
        List<TransactionHistoryDTO> history = fetchUserTransactionHistory(userId);
        return applyFiltersAndSort(history, filters);
    }

    // decide which user's data to query
    private Long resolveQueryUser(TransactionFilterRequest filters, User currentUser) {
        boolean isEmployee = "EMPLOYEE".equalsIgnoreCase(currentUser.getRole());
       // If employee and they selected a specific customer
        if (isEmployee && filters.getUserId() != null) return filters.getUserId();
         // If employee but no customer selected - return null (means all customers)
        if (isEmployee) return null;
         // If customer - always return their own
        return currentUser.getId();
    }

    // Get transactions 
    private List<TransactionHistoryDTO> fetchUserTransactionHistory(Long userId) {
        List<TransactionHistoryDTO> result = new ArrayList<>();
        if (userId == null) {
             // Employee viewing all transactions
            result.addAll(transactionRepository.findAll().stream().map(this::toTransactionDTO).toList());
            result.addAll(atmOperationRepository.findAll().stream().map(this::toAtmDTO).toList());
        } else {
            // Specific user's transactions
            result.addAll(transactionRepository
                .findByFromAccount_User_IdOrToAccount_User_Id(userId, userId)
                .stream().map(this::toTransactionDTO).toList());
            result.addAll(atmOperationRepository
                .findByAccount_User_Id(userId)
                .stream().map(this::toAtmDTO).toList());
        }
        return result;
    }

    // Filter + sort descending by timestamp
    private List<TransactionHistoryDTO> applyFiltersAndSort(List<TransactionHistoryDTO> transactions, TransactionFilterRequest filters) {
        // Parse date strings to DateTime objects
        LocalDateTime start = parseDate(filters.getStartDate(), true);
        LocalDateTime end = parseDate(filters.getEndDate(), false);
        return transactions.stream()
         // Filter by date range
            .filter(tx -> isWithinRange(tx.getTimestamp(), start, end))
            // Filter by amount range
            .filter(tx -> isWithinAmount(tx.getAmount(), filters.getMinAmount(), filters.getMaxAmount()))
           // Sort newest first
            .sorted(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed())
            .collect(Collectors.toList());
    }

    // Save a transaction entity
    private void saveTransaction(Account sender, Account receiver, BigDecimal amount, String desc, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setFromAccount(sender);
        tx.setToAccount(receiver);
        tx.setAmount(amount);
        tx.setDescription(desc);
        tx.setTransactionType(type);
        transactionRepository.save(tx);
    }

    // Transfer via char[] IBAN with security
    @Transactional
    public void processTransfer(TransferRequest req) {
        validateAmount(req.getAmount());
        if (req.getSenderIban() == null || req.getReceiverIban() == null) {
            throw new IllegalArgumentException("IBANs required");
        }

        char[] senderIbanChars = req.getSenderIban();
        char[] receiverIbanChars = req.getReceiverIban();
        String senderIban = new String(senderIbanChars);
        String receiverIban = new String(receiverIbanChars);
        Arrays.fill(senderIbanChars, '0');
        Arrays.fill(receiverIbanChars, '0');

        if (!IbanGenerator.validateIban(senderIban)) throw new IllegalArgumentException("Invalid sender IBAN");
        if (!IbanGenerator.validateIban(receiverIban)) throw new IllegalArgumentException("Invalid receiver IBAN");
        if (senderIban.equals(receiverIban)) throw new IllegalArgumentException("Cannot transfer to self");

        Account sender = getValidAccountByIban(senderIban);
        Account receiver = getValidAccountByIban(receiverIban);
        transferMoney(sender.getId(), receiver.getId(), req.getAmount(), req.getDescription());
    }

    // Transfer money between 2 accounts by ID
    @Transactional
    public void transferMoney(Long senderId, Long receiverId, BigDecimal amount, String desc) {
        validateAmount(amount);
        if (senderId.equals(receiverId)) throw new IllegalArgumentException("Cannot transfer to self");

        Account sender = getValidAccount(senderId, true);
        Account receiver = getValidAccount(receiverId, true);
        if (sender.getBalance().compareTo(amount) < 0) throw new IllegalArgumentException("Insufficient balance");

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        saveTransaction(sender, receiver, amount, desc, TransactionType.TRANSFER);
    }

    // Deposit funds into approved account
    @Transactional
    public void depositMoney(Long accountId, BigDecimal amount) {
        validateAmount(amount);
        Account account = getValidAccount(accountId, true);
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        saveTransaction(null, account, amount, "Deposit", TransactionType.DEPOSIT);
    }

    // Convert transaction History to DTO
    private TransactionHistoryDTO toTransactionDTO(Transaction tx) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(tx.getId());
        dto.setAmount(tx.getAmount());
        dto.setTimestamp(tx.getTimestamp());
        dto.setDescription(tx.getDescription());
        dto.setTransactionType(tx.getTransactionType().name());
  // Sender info (if exists)
        if (tx.getFromAccount() != null) {
            dto.setSenderAccountId(tx.getFromAccount().getId());
            dto.setFromAccountIban(tx.getFromAccount().getIban());
            dto.setFromAccountHolderName(Optional.ofNullable(tx.getFromAccount().getUser()).map(User::getName).orElse(""));// Empty string if no user
        }
// Receiver info
        if (tx.getToAccount() != null) {
            dto.setReceiverAccountId(tx.getToAccount().getId());
            dto.setToAccountIban(tx.getToAccount().getIban());
            dto.setToAccountHolderName(Optional.ofNullable(tx.getToAccount().getUser()).map(User::getName).orElse(""));
        }

        return dto;
    }

    // Convert ATM op to DTO
    private TransactionHistoryDTO toAtmDTO(AtmOperation op) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(op.getId());
        dto.setAmount(op.getAmount());
        dto.setTimestamp(op.getTimestamp());
        dto.setDescription("ATM " + op.getOperationType());
        dto.setTransactionType(op.getOperationType().name());

        if (op.getAccount() != null) {
            String name = Optional.ofNullable(op.getAccount().getUser()).map(User::getName).orElse("");
            if (op.getOperationType() == AtmOperation.OperationType.DEPOSIT) {
                dto.setReceiverAccountId(op.getAccount().getId());
                dto.setToAccountIban(op.getAccount().getIban());
                dto.setToAccountHolderName(name);
            } else {
                dto.setSenderAccountId(op.getAccount().getId());
                dto.setFromAccountIban(op.getAccount().getIban());
                dto.setFromAccountHolderName(name);
            }
        }

        return dto;
    }

    // Validate amount is positive
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    // Get account and ensure it's valid
    private Account getValidAccount(Long id, boolean requireApproved) {
        Account acc = accountRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (requireApproved && !acc.isApproved()) throw new IllegalArgumentException("Account not approved");
        if (acc.isClosed()) throw new IllegalArgumentException("Account closed");
        return acc;
    }

    // Get account by IBAN string
    private Account getValidAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
            .orElseThrow(() -> new IllegalArgumentException("Account with IBAN not found"));
    }

    // Parse request date
    private LocalDateTime parseDate(String dateStr, boolean isStart) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(dateStr, DATE_FORMATTER);
            return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date format. Use yyyy-MM-dd");
        }
    }

    // Date filter check
    private boolean isWithinRange(LocalDateTime ts, LocalDateTime start, LocalDateTime end) {
        return (start == null || !ts.isBefore(start)) &&
               (end == null || !ts.isAfter(end));
    }

    // Amount filter check
    private boolean isWithinAmount(BigDecimal amt, BigDecimal min, BigDecimal max) {
        return (min == null || amt.compareTo(min) >= 0) &&
               (max == null || amt.compareTo(max) <= 0);
    }
}
