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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AtmService {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    
    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    @Autowired
    private PinHashUtil pinHashUtil;
    
    /**
     * Process a deposit to an account via ATM
     * @param accountId the ID of the account to deposit to
     * @param amount the amount to deposit
     * @param pin the PIN for verification
     * @return the ATM operation record
     * @throws ResourceNotFoundException if the account is not found
     * @throws InvalidPinException if the PIN is invalid
     * @throws IllegalStateException if the account is not approved or is closed
     * @throws IllegalArgumentException if the amount is zero or negative
     */
    @Transactional
    public AtmOperation processDeposit(Long accountId, BigDecimal amount, String pin) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        // Verify PIN
        CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));
        
        if (!pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())) {
            throw new InvalidPinException("Invalid PIN provided");
        }
        
        // Check account status
        if (!account.isApproved()) {
            throw new IllegalStateException("Account is not approved for transactions");
        }
        
        if (account.isClosed()) {
            throw new IllegalStateException("Account is closed and cannot accept deposits");
        }
        
        // Process deposit
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        
        // Create ATM operation record
        AtmOperation operation = new AtmOperation();
        operation.setAccount(account);
        operation.setAmount(amount);
        operation.setOperationType(AtmOperation.OperationType.DEPOSIT);
        operation.setTimestamp(LocalDateTime.now());
        
        return atmOperationRepository.save(operation);
    }
    
    /**
     * Process a withdrawal from an account via ATM
     * @param accountId the ID of the account to withdraw from
     * @param amount the amount to withdraw
     * @param pin the PIN for verification
     * @return the ATM operation record
     * @throws ResourceNotFoundException if the account is not found
     * @throws InvalidPinException if the PIN is invalid
     * @throws IllegalStateException if the account is not approved or is closed
     * @throws IllegalArgumentException if the amount is zero or negative, or if the account has insufficient balance
     */
    @Transactional
    public AtmOperation processWithdrawal(Long accountId, BigDecimal amount, String pin) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be greater than zero");
        }
        
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        // Verify PIN
        CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));
        
        if (!pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())) {
            throw new InvalidPinException("Invalid PIN provided");
        }
        
        // Check account status
        if (!account.isApproved()) {
            throw new IllegalStateException("Account is not approved for transactions");
        }
        
        if (account.isClosed()) {
            throw new IllegalStateException("Account is closed and cannot process withdrawals");
        }
        
        // Check balance
        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance for withdrawal");
        }
        
        // Process withdrawal
        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        
        // Create ATM operation record
        AtmOperation operation = new AtmOperation();
        operation.setAccount(account);
        operation.setAmount(amount);
        operation.setOperationType(AtmOperation.OperationType.WITHDRAW);
        operation.setTimestamp(LocalDateTime.now());
        
        return atmOperationRepository.save(operation);
    }
    
    /**
     * Get ATM operations for an account
     * @param accountId the ID of the account to get operations for
     * @return the list of ATM operations
     */
    public List<AtmOperation> getAtmOperations(Long accountId) {
        return atmOperationRepository.findByAccount_Id(accountId);
    }
}