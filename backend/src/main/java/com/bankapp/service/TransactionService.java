package com.bankapp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.exception.UserAccessDeniedException ;
import com.bankapp.exception.UnapprovedAccountException;
import com.bankapp.model.*;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.*;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor // Auto-generates constructor for final fields
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AtmOperationRepository atmOperationRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //  Transfer Logic ----------------

    // Validates and processes a transfer via IBANs
    @Transactional
    public void processTransfer(TransferRequest request) {
        validateTransferRequest(request);
        Account sender = getAccountByIban(request.getSenderIban());
        Account receiver = getAccountByIban(request.getReceiverIban());
        validateAccountsForTransfer(sender, receiver, request.getAmount());
        updateBalances(sender, receiver, request.getAmount());
        saveTransaction(sender, receiver, request.getAmount(), request.getDescription(), TransactionType.TRANSFER);
    }

    // Verifies transfer request structure and amount
    private void validateTransferRequest(TransferRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
        if (request.getSenderIban() == null || request.getReceiverIban() == null) {
            throw new IllegalArgumentException("Both sender and receiver IBANs are required");
        }
    }
    
    // Validates account before transfer 
    private void validateAccountsForTransfer(Account sender, Account receiver, BigDecimal amount) {
        if (!sender.isApproved() || !receiver.isApproved()) {
            throw new IllegalArgumentException("Both accounts must be approved");
        }
        if (sender.isClosed() || receiver.isClosed()) {
            throw new IllegalArgumentException("Cannot transfer with closed accounts");
        }
        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    // Updates the new balance of sender and receiver
    private void updateBalances(Account sender, Account receiver, BigDecimal amount) {
        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.saveAll(List.of(sender, receiver));
    }

    // Centralized method for saving transactions
    private void saveTransaction(Account from, Account to, BigDecimal amount, String description, TransactionType type) {
        Transaction tx = new Transaction();
        tx.setFromAccount(from);
        tx.setToAccount(to);
        tx.setAmount(amount);
        tx.setDescription(description);
        tx.setTransactionType(type);
        transactionRepository.save(tx);
    }

    // History Retrieval ----------------

    // Get user transaction history without filters
    public List<TransactionHistoryDTO> getUserTransactionHistory(Long userId) {
        return getUserTransactionHistory(userId, null, null, null, null);
    }
    
    // Get user transaction history with optional date and amount filters
    public List<TransactionHistoryDTO> getUserTransactionHistory(
            Long userId, String start, String end, BigDecimal min, BigDecimal max) {

        List<Transaction> txs = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
        List<AtmOperation> atms = atmOperationRepository.findByAccount_User_Id(userId);

        return combineAndFilter(txs, atms, start, end, min, max);
    }

    // Get account transaction history without filters
    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(
            String iban, String start, String end, BigDecimal min, BigDecimal max) {

        Account account = getAccountByIban(iban);
        return getUserTransactionHistory(account.getUser().getId(), start, end, min, max);
    }
    
    // Get account transaction history with optional date and amount filters
    public List<TransactionHistoryDTO> getTransactionHistoryByIbanWithAuth(
            String iban, User requester, String start, String end, BigDecimal min, BigDecimal max) {

        Account account = getAccountByIban(iban);
        assertUserCanAccess(requester, account.getUser().getId());
        return getAccountTransactionHistoryByIban(iban, start, end, min, max);
    }

    public List<TransactionHistoryDTO> getTransactionsByUserWithAuth(
            Long userId, User requester, String start, String end, BigDecimal min, BigDecimal max) {

        assertUserCanAccess(requester, userId);
        return getUserTransactionHistory(userId, start, end, min, max);
    }

    // Helpers ----------------

    private Account getAccountById(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }

    private Account getAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
                .orElseThrow(() -> new IllegalArgumentException("Account not found for IBAN: " + iban));
    }

    private void assertUserCanAccess(User requester, Long ownerId) {
        if (!Objects.equals(requester.getId(), ownerId) && 
            !requester.getRole().equalsIgnoreCase("EMPLOYEE")) {
            throw new UserAccessDeniedException("Access denied");
        }
    }

    private List<TransactionHistoryDTO> combineAndFilter(
            List<Transaction> txs, List<AtmOperation> atms,
            String start, String end, BigDecimal min, BigDecimal max) {

        List<TransactionHistoryDTO> all = Stream.concat(
                txs.stream().map(this::mapTransactionToDTO),
                atms.stream().map(this::mapAtmOperationToDTO)
        )
        .sorted(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed())
        .collect(Collectors.toList());

        return filterTransactions(all, start, end, min, max);
    }

    private List<TransactionHistoryDTO> filterTransactions(
            List<TransactionHistoryDTO> txs,
            String startStr, String endStr,
            BigDecimal min, BigDecimal max) {

        LocalDateTime start = parseDate(startStr, true);
        LocalDateTime end = parseDate(endStr, false);

        return txs.stream()
                .filter(tx -> (start == null || !tx.getTimestamp().isBefore(start)) &&
                              (end == null || !tx.getTimestamp().isAfter(end)) &&
                              (min == null || tx.getAmount().compareTo(min) >= 0) &&
                              (max == null || tx.getAmount().compareTo(max) <= 0))
                .collect(Collectors.toList());
    }

    private LocalDateTime parseDate(String str, boolean isStart) {
        if (str == null || str.isEmpty()) return null;
        try {
            LocalDate date = LocalDate.parse(str, DATE_FORMATTER);
            return isStart ? date.atStartOfDay() : date.atTime(LocalTime.MAX);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Invalid date: " + str);
        }
    }

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
}
