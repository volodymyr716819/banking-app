package com.bankapp.service;

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.AtmOperation.OperationType;
import com.bankapp.model.*;
import com.bankapp.repository.*;
import com.bankapp.util.PinHashUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class AtmService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private AtmOperationRepository atmOperationRepository;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private PinHashUtil pinHashUtil;

    // Executes deposit or withdrawal
    @Transactional
    public AtmOperation performAtmOperation(Long accountId, BigDecimal amount, String pin, OperationType type) {
        validateAmount(amount);
        Account account = loadAndValidateAccountAndPin(accountId, pin);
        checkAccountStatus(account);

        if (type == OperationType.DEPOSIT) {
            enforceDailyDepositLimit(account, amount);
        }

        updateBalance(account, amount, type);
        return saveOperation(account, amount, type);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private void enforceDailyDepositLimit(Account account, BigDecimal amount) {
        BigDecimal todayTotal = transactionRepository.sumDepositsForToday(account.getId());
        if (todayTotal.add(amount).compareTo(account.getDailyLimit()) > 0) {
            throw new IllegalArgumentException("Daily deposit limit exceeded");
        }
    }

    private Account loadAndValidateAccountAndPin(Long accountId, String pin) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        CardDetails card = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));

        if (!pinHashUtil.verifyPin(pin, card.getHashedPin())) {
            throw new InvalidPinException("Invalid PIN");
        }

        return account;
    }

    private void checkAccountStatus(Account account) {
        if (!account.isApproved()) {
            throw new IllegalStateException("Account is not approved");
        }
        if (account.isClosed()) {
            throw new IllegalStateException("Account is closed");
        }
    }

    private void updateBalance(Account account, BigDecimal amount, OperationType type) {
        if (type == OperationType.WITHDRAW && account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        BigDecimal newBalance = (type == OperationType.DEPOSIT)
                ? account.getBalance().add(amount)
                : account.getBalance().subtract(amount);

        account.setBalance(newBalance);
        accountRepository.save(account);
    }

    private AtmOperation saveOperation(Account account, BigDecimal amount, OperationType type) {
        AtmOperation op = new AtmOperation();
        op.setAccount(account);
        op.setAmount(amount);
        op.setOperationType(type);
        op.setTimestamp(LocalDateTime.now());
        return atmOperationRepository.save(op);
    }

    public List<AtmOperation> getAtmOperations(Long accountId) {
        return atmOperationRepository.findByAccount_Id(accountId);
    }

    public ResponseEntity<?> getBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .filter(Account::isApproved)
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(account.getBalance()))
                .orElse(ResponseEntity.status(404).body("Account not found or not approved"));
    }

    public ResponseEntity<?> getPinStatus(Long accountId) {
        boolean pinCreated = cardDetailsRepository.findByAccountId(accountId)
                .map(CardDetails::isPinCreated)
                .orElse(false);
        return ResponseEntity.ok(Map.of("pinCreated", pinCreated));
    }
}
