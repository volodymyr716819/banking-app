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

    // mapping regular transactions to DTO
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

    // mapping ATM operations to DTO
    private TransactionHistoryDTO mapAtmOperationToDTO(AtmOperation atm) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(atm.getId());
        dto.setAmount(atm.getAmount());
        dto.setTimestamp(atm.getTimestamp());
        dto.setDescription("ATM " + atm.getOperationType());
        dto.setTransactionType(atm.getOperationType().toString());
        return dto;
    }

    public List<TransactionHistoryDTO> getUserTransactionHistory(Long userId) {
        List<Transaction> transactions = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_User_Id(userId);

        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(this::mapTransactionToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(this::mapAtmOperationToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);

        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());

        return combinedHistory;
    }

    public List<TransactionHistoryDTO> getAccountTransactionHistory(Long accountId) {
        List<Transaction> transactions = transactionRepository.findByFromAccount_IdOrToAccount_Id(accountId, accountId);
        List<AtmOperation> atmOperations = atmOperationRepository.findByAccount_Id(accountId);

        List<TransactionHistoryDTO> transactionHistory = transactions.stream()
                .map(this::mapTransactionToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> atmHistory = atmOperations.stream()
                .map(this::mapAtmOperationToDTO)
                .collect(Collectors.toList());

        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionHistory);
        combinedHistory.addAll(atmHistory);

        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());

        return combinedHistory;
    }

    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(String iban) {
        Optional<Account> accountOpt = accountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for IBAN: " + iban);
        }
        return getAccountTransactionHistory(accountOpt.get().getId());
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

        if (!sender.isApproved() || !receiver.isApproved()) {
            throw new IllegalArgumentException("Both accounts must be approved for transactions");
        }

        if (sender.isClosed() || receiver.isClosed()) {
            throw new IllegalArgumentException("Closed accounts cannot process transactions");
        }

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

    @Transactional
    public void transferMoneyByIban(String senderIban, String receiverIban, BigDecimal amount, String description) {
        if (!IbanGenerator.validateIban(senderIban)) {
            throw new IllegalArgumentException("Invalid sender IBAN");
        }

        if (!IbanGenerator.validateIban(receiverIban)) {
            throw new IllegalArgumentException("Invalid receiver IBAN");
        }

        Optional<Account> senderOpt = accountRepository.findByIban(senderIban);
        Optional<Account> receiverOpt = accountRepository.findByIban(receiverIban);

        if (senderOpt.isEmpty()) {
            throw new IllegalArgumentException("Sender account not found");
        }

        if (receiverOpt.isEmpty()) {
            throw new IllegalArgumentException("Receiver account not found");
        }

        transferMoney(senderOpt.get().getId(), receiverOpt.get().getId(), amount, description);
    }

    public List<TransactionHistoryDTO> getTransactionHistoryByIbanWithAuth(String iban, User requester) {
        Account account = accountRepository.findByIban(iban)
        .orElseThrow(() -> new IllegalArgumentException("Account not found"));

        if (!account.getUser().getId().equals(requester.getId()) &&
           !requester.getRole().equalsIgnoreCase("EMPLOYEE")) {
           throw new IllegalArgumentException("Access denied");
        }

         return getAccountTransactionHistory(account.getId());
        }

    public List<TransactionHistoryDTO> getTransactionsByUserWithAuth(Long userId, User requester) {
        if (!requester.getId().equals(userId) && !requester.getRole().equalsIgnoreCase("EMPLOYEE")) {
            throw new IllegalArgumentException("Access denied");
        }

        return getUserTransactionHistory(userId);
    }
}
