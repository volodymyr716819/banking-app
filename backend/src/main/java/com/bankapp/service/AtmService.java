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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service that handles ATM operations
 * 
 * Provides functionality for ATM transactions including
 * PIN verification, deposits, withdrawals, and balance inquiries
 */
@Service
public class AtmService {
    // Logger for PIN-related security events
    private static final Logger logger = LoggerFactory.getLogger(AtmService.class);

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
        
        // Fetch and validate account existence
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        // Verify the PIN is correct
        verifyPin(accountId, pin);
        
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
        
        // Fetch and validate account existence
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        // Verify the PIN is correct
        verifyPin(accountId, pin);
        
        // Check account status
        if (!account.isApproved()) {
            throw new IllegalStateException("Account is not approved for transactions");
        }
        
        if (account.isClosed()) {
            throw new IllegalStateException("Account is closed and cannot process withdrawals");
        }
        
        // Check for sufficient funds
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

    public ResponseEntity<?> getBalance(Long accountId) {
        return accountRepository.findById(accountId)
                .filter(Account::isApproved)
                .<ResponseEntity<?>>map(account -> ResponseEntity.ok(account.getBalance()))
                .orElse(ResponseEntity.status(404).body("Account not found or not approved"));
    }

    /**
     * Check if a PIN has been created for an account
     * 
     * This method is used by the ATM interface to determine if
     * a PIN needs to be created or just verified
     * 
     * @param accountId Account to check PIN status for
     * @return Response with pinCreated status (true/false)
     */
    public ResponseEntity<?> getPinStatus(Long accountId) {
        // Find card details for this account
        boolean pinCreated = cardDetailsRepository.findByAccountId(accountId)
                // Check if PIN has been created
                .map(CardDetails::isPinCreated)
                // Default to false if no card details found
                .orElse(false);
        
        // Return simple response with PIN status
        return ResponseEntity.ok().body(java.util.Map.of("pinCreated", pinCreated));
    }
    
    /**
     * Verify that a PIN is correct for an account
     * 
     * This helper method encapsulates the PIN verification logic
     * to avoid code duplication across transaction methods
     *
     * @param accountId ID of the account to verify PIN for
     * @param pin PIN to verify
     * @throws ResourceNotFoundException if card details not found
     * @throws InvalidPinException if PIN is incorrect
     */
    private void verifyPin(Long accountId, String pin) {
        // Look up card details for the account
        CardDetails cardDetails = cardDetailsRepository.findByAccountId(accountId)
                .orElseThrow(() -> {
                    logger.warn("PIN verification failed: Card details not found for account {}", accountId);
                    return new ResourceNotFoundException("CardDetails", "accountId", accountId);
                });
        
        // Make sure PIN has been set
        if (!cardDetails.isPinCreated()) {
            logger.warn("PIN verification failed: PIN not set for account {}", accountId);
            throw new InvalidPinException("PIN not set for this account");
        }
        
        // Verify the PIN matches
        if (!pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())) {
            // Log failed attempt (security measure)
            logger.warn("PIN verification failed: Invalid PIN provided for account {}", accountId);
            throw new InvalidPinException("Invalid PIN provided");
        }
        
        // PIN is valid - verification successful
        logger.debug("PIN verification successful for account {}", accountId);
    }
}