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
import com.bankapp.model.User;
import com.bankapp.dto.TransferRequest;
import com.bankapp.dto.PinRequest;
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

    // Converts a Transaction entity into a DTO for frontend use
    private TransactionHistoryDTO mapTransactionToDTO(Transaction t) {
        // Create basic transaction info
        TransactionHistoryDTO dto = createBasicTransactionDTO(t.getId(), t.getAmount(), 
                                                             t.getTimestamp(), t.getDescription());
        
        // Set account IDs if they exist
        setAccountDetails(dto, t.getFromAccount(), t.getToAccount());
        
        // Set transaction type
        String txType = (t.getTransactionType() != null) ? 
                        t.getTransactionType().toString() : "UNKNOWN";
        dto.setTransactionType(txType);
        
        return dto;
    }

    // Converts an ATM operation (withdraw/deposit) into a DTO
    private TransactionHistoryDTO mapAtmOperationToDTO(AtmOperation atm) {
        // Create basic transaction info
        TransactionHistoryDTO dto = createBasicTransactionDTO(atm.getId(), atm.getAmount(), 
                                                             atm.getTimestamp(), 
                                                             "ATM " + atm.getOperationType());
        
        // Set transaction type
        dto.setTransactionType(atm.getOperationType().toString());
        
        // Set account details for ATM operations
        if (atm.getAccount() != null) {
            // For deposits, set as receiver
            if (atm.getOperationType().toString().equals("DEPOSIT")) {
                dto.setToAccountIban(atm.getAccount().getIban());
                dto.setToAccountHolderName(atm.getAccount().getUser().getName());
                dto.setReceiverAccountId(atm.getAccount().getId());
            } 
            // For withdrawals, set as sender
            else if (atm.getOperationType().toString().equals("WITHDRAW")) {
                dto.setFromAccountIban(atm.getAccount().getIban());
                dto.setFromAccountHolderName(atm.getAccount().getUser().getName());
                dto.setSenderAccountId(atm.getAccount().getId());
            }
        }
        
        return dto;
    }
    
    // Creates a DTO with the basic shared transaction properties
    private TransactionHistoryDTO createBasicTransactionDTO(Long id, BigDecimal amount, 
                                                          LocalDateTime timestamp, String description) {
        TransactionHistoryDTO dto = new TransactionHistoryDTO();
        dto.setTransactionId(id);
        dto.setAmount(amount);
        dto.setTimestamp(timestamp);
        dto.setDescription(description);
        return dto;
    }
    
    // Sets account details on a transaction DTO
    private void setAccountDetails(TransactionHistoryDTO dto, Account fromAccount, Account toAccount) {
        // Set sender account details if it exists
        if (fromAccount != null) {
            dto.setSenderAccountId(fromAccount.getId());
            dto.setFromAccountIban(fromAccount.getIban());
            dto.setFromAccountHolderName(fromAccount.getUser().getName());
        }
        
        // Set receiver account details if it exists
        if (toAccount != null) {
            dto.setReceiverAccountId(toAccount.getId());
            dto.setToAccountIban(toAccount.getIban());
            dto.setToAccountHolderName(toAccount.getUser().getName());
        }
    }

    // Retrieves all transactions and ATM operations for a given user
    public List<TransactionHistoryDTO> getUserTransactionHistory(Long userId) {
        // Get database transactions and ATM operations
        List<Transaction> transactions = fetchUserTransactions(userId);
        List<AtmOperation> atmOperations = fetchUserAtmOperations(userId);
        
        // Convert to DTOs and combine
        return combineAndSortTransactionHistory(transactions, atmOperations);
    }
    
    // Fetches all transactions for a specific user
    private List<Transaction> fetchUserTransactions(Long userId) {
        // Fetch both sent and received transactions
        List<Transaction> transactions = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(userId, userId);
        
        // Log transaction count and first transaction if available
        System.out.println("Found " + transactions.size() + " transactions for user " + userId);
        if (!transactions.isEmpty()) {
            Transaction firstTx = transactions.get(0);
            System.out.println("Sample transaction: ID=" + firstTx.getId() + 
                              ", from=" + (firstTx.getFromAccount() != null ? firstTx.getFromAccount().getId() : "null") + 
                              ", to=" + (firstTx.getToAccount() != null ? firstTx.getToAccount().getId() : "null") + 
                              ", amount=" + firstTx.getAmount());
        }
        
        return transactions;
    }
    
    // Fetches all ATM operations for a specific user
    private List<AtmOperation> fetchUserAtmOperations(Long userId) {
        return atmOperationRepository.findByAccount_User_Id(userId);
    }
    
    // Combines and sorts transaction and ATM operation history
    private List<TransactionHistoryDTO> combineAndSortTransactionHistory(
            List<Transaction> transactions, List<AtmOperation> atmOperations) {
        
        // Convert transactions to DTOs
        List<TransactionHistoryDTO> transactionDTOs = transactions.stream()
                .map(this::mapTransactionToDTO)
                .collect(Collectors.toList());
        
        // Convert ATM operations to DTOs
        List<TransactionHistoryDTO> atmDTOs = atmOperations.stream()
                .map(this::mapAtmOperationToDTO)
                .collect(Collectors.toList());
        
        // Combine both types
        List<TransactionHistoryDTO> combinedHistory = new ArrayList<>();
        combinedHistory.addAll(transactionDTOs);
        combinedHistory.addAll(atmDTOs);
        
        // Sort newest to oldest
        combinedHistory.sort(Comparator.comparing(TransactionHistoryDTO::getTimestamp).reversed());
        
        return combinedHistory;
    }

    // Retrieves all transactions and ATM operations for a specific account
    public List<TransactionHistoryDTO> getAccountTransactionHistory(Long accountId) {
        // Get database transactions and ATM operations
        List<Transaction> transactions = fetchAccountTransactions(accountId);
        List<AtmOperation> atmOperations = fetchAccountAtmOperations(accountId);
        
        // Convert to DTOs and combine
        return combineAndSortTransactionHistory(transactions, atmOperations);
    }
    
    // Fetches all transactions for a specific account
    private List<Transaction> fetchAccountTransactions(Long accountId) {
        // Fetch both sent and received transactions
        List<Transaction> transactions = transactionRepository.findByFromAccount_IdOrToAccount_Id(accountId, accountId);
        
        // Log transaction count and first transaction if available
        System.out.println("Found " + transactions.size() + " transactions for account " + accountId);
        if (!transactions.isEmpty()) {
            Transaction firstTx = transactions.get(0);
            System.out.println("Sample transaction: ID=" + firstTx.getId() + 
                              ", from=" + (firstTx.getFromAccount() != null ? firstTx.getFromAccount().getIban() : "null") + 
                              ", to=" + (firstTx.getToAccount() != null ? firstTx.getToAccount().getIban() : "null") + 
                              ", amount=" + firstTx.getAmount());
        }
        
        return transactions;
    }
    
    // Fetches all ATM operations for a specific account
    private List<AtmOperation> fetchAccountAtmOperations(Long accountId) {
        return atmOperationRepository.findByAccount_Id(accountId);
    }

    // Converts IBAN to account and returns history for it
    public List<TransactionHistoryDTO> getAccountTransactionHistoryByIban(String iban) {
        Optional<Account> accountOpt = accountRepository.findByIban(iban);
        if (accountOpt.isEmpty()) {
            throw new IllegalArgumentException("Account not found for IBAN: " + iban);
        }
        return getAccountTransactionHistory(accountOpt.get().getId());
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

    // Returns transaction history for a given IBAN if requester is authorized
    public List<TransactionHistoryDTO> getTransactionHistoryByIbanWithAuth(String iban, User requester) {
        // Find the account by IBAN
        Account account = findAccountByIban(iban);
        
        // Check if user has permission to view this account
        checkAccountAccessPermission(account, requester);
        
        // Get the transaction history
        return getAccountTransactionHistory(account.getId());
    }
    
    // Find account by IBAN or throw exception
    private Account findAccountByIban(String iban) {
        return accountRepository.findByIban(iban)
            .orElseThrow(() -> new IllegalArgumentException("Account not found for IBAN: " + iban));
    }
    
    // Check if user has permission to access account
    private void checkAccountAccessPermission(Account account, User requester) {
        boolean isOwner = account.getUser().getId().equals(requester.getId());
        boolean isEmployee = requester.getRole().equalsIgnoreCase("EMPLOYEE");
        
        if (!isOwner && !isEmployee) {
            throw new IllegalArgumentException("Access denied");
        }
    }
    
    // Returns full transaction history for a user with access check
    public List<TransactionHistoryDTO> getTransactionsByUserWithAuth(Long userId, User requester) {
        // Check if user has permission to view transactions
        checkUserAccessPermission(userId, requester);
        
        // Get the transaction history
        return getUserTransactionHistory(userId);
    }
    
    // Check if user has permission to access user data
    private void checkUserAccessPermission(Long userId, User requester) {
        boolean isSameUser = requester.getId().equals(userId);
        boolean isEmployee = requester.getRole().equalsIgnoreCase("EMPLOYEE");
        
        if (!isSameUser && !isEmployee) {
            throw new IllegalArgumentException("Access denied");
        }
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
}
