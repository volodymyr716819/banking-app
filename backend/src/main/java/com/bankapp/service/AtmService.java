package com.bankapp.service;
import java.util.Map;

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AtmService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private AtmOperationRepository atmOperationRepository;
    @Autowired private PinHashUtil pinHashUtil;

    // common method for both deposit and withdrawal, validates account, PIN, and sufficient funds before applying transaction
    @Transactional
    public AtmOperation performAtmOperation(Long accountId, BigDecimal amount, char[] pin, AtmOperation.OperationType type) {
        ValidationContext context = validateAtmOperation(accountId, amount, pin, type);
        return executeAtmOperation(context.account, amount, type);
    }

    // returns all ATM transactions for an account
    public List<AtmOperation> getAtmOperations(Long accountId) {
        return atmOperationRepository.findByAccount_Id(accountId);
    }

    // validates account status, PIN, and balance rules, returns context with loaded and verified entities
    private ValidationContext validateAtmOperation(Long accountId, BigDecimal amount, char[] pin, AtmOperation.OperationType type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
           throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
            .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));

        if (!pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())) {
            throw new InvalidPinException("Invalid PIN provided");
        }

        if (!account.isApproved()) {
           throw new IllegalStateException("Account is not approved for transactions");
        }

        if (account.isClosed()) {
           throw new IllegalStateException("Account is closed and cannot process transactions");
        }

        if (type == AtmOperation.OperationType.WITHDRAW && account.getBalance().compareTo(amount) < 0) {
           throw new IllegalArgumentException("Insufficient balance for withdrawal");
        }

        return new ValidationContext(account, cardDetails);
    }

    // performs balance update and persists ATM operation
    private AtmOperation executeAtmOperation(Account account, BigDecimal amount, AtmOperation.OperationType type) {
        BigDecimal updatedBalance = (type == AtmOperation.OperationType.DEPOSIT)
            ? account.getBalance().add(amount)
            : account.getBalance().subtract(amount);

        account.setBalance(updatedBalance);
        accountRepository.save(account);

        AtmOperation operation = new AtmOperation();
        operation.setAccount(account);
        operation.setAmount(amount);
        operation.setOperationType(type);
        operation.setTimestamp(LocalDateTime.now());

        return atmOperationRepository.save(operation);
    }

    // internal holder to pass validated account & card as a pair
    private record ValidationContext(Account account, CardDetails cardDetails) {}

    // returns current balance if account is approved
    public ResponseEntity<?> getBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .filter(Account::isApproved)
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(account.getBalance()))
                .orElse(ResponseEntity.status(404).body("Account not found or not approved"));
    }

    // returns current balance if account is approved and PIN is valid
    public ResponseEntity<?> getBalanceWithPin(Long accountId, char[] pin) {
        try {
            // First validate the PIN
            CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));
            
            if (!pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())) {
                throw new InvalidPinException("Invalid PIN provided");
            }
            
            // If PIN is valid, return the balance
            return accountRepository.findById(accountId)
                    .filter(Account::isApproved)
                    .<ResponseEntity<?>>map(account -> ResponseEntity.ok(account.getBalance()))
                    .orElse(ResponseEntity.status(404).body("Account not found or not approved"));
        } catch (InvalidPinException e) {
            return ResponseEntity.status(400).body("Invalid PIN");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // indicates whether a PIN has been created for the account
    public ResponseEntity<?> getPinStatus(Long accountId) {
    if (accountId == null) {
        return ResponseEntity.badRequest().body("Missing accountId");
    }

    boolean pinCreated = cardDetailsRepository.findByAccountId(accountId)
            .map(CardDetails::isPinCreated)
            .orElse(false);

    return ResponseEntity.ok().body(Map.of("pinCreated", pinCreated));
}

}
