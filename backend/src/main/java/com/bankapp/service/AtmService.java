// File: com.bankapp.service.AtmService.java

package com.bankapp.service;

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

    @Transactional
    public AtmOperation performAtmOperation(Long accountId, BigDecimal amount, char[] pin, AtmOperation.OperationType type) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));

        CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));

        if (!pinHashUtil.verifyPin(new String(pin), cardDetails.getHashedPin())) {
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
        return ResponseEntity.ok().body(java.util.Map.of("pinCreated", pinCreated));
    }
}
