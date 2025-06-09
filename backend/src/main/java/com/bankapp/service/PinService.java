package com.bankapp.service;

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.CardDetails;
import com.bankapp.dto.PinRequest;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

/**
 * Service that handles all bank card PIN operations
 * 
 * This service manages PIN creation, verification, and changing for
 * customer bank accounts. It ensures secure handling of PINs by
 * using character arrays and secure hashing.
 */
@Service
public class PinService {

    // Dependencies
    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private PinHashUtil pinHashUtil;
    
    // Constants
    private static final int PIN_LENGTH = 4;
    private static final char PIN_MASK_CHAR = '0';
    
    /**
     * Check if a PIN has been created for the given account
     * 
     * @param accountId ID of the account to check
     * @return true if PIN exists, false otherwise
     */
    public boolean checkPinStatus(Long accountId) {
        return findCardDetails(accountId)
                .map(CardDetails::isPinCreated)
                .orElse(false);
    }

    /**
     * Create a new PIN for an account
     * 
     * @param pinRequest Request containing account ID and PIN
     * @throws IllegalArgumentException if PIN is invalid
     * @throws RuntimeException if account not found
     */
    public void createPin(PinRequest pinRequest) {
        // Extract and validate PIN
        char[] pinChars = pinRequest.getPin();
        validatePinFormat(pinChars);
        
        // Find the account
        Account account = findAccount(pinRequest.getAccountId());
        
        // Remove any existing card details for this account
        removeExistingCardDetails(account.getId());
        
        // Process PIN securely
        String pin = extractPinAndClear(pinChars);
        String hashedPin = pinHashUtil.hashPin(pin);
        
        // Create and save new card details
        saveNewCardDetails(account, hashedPin);
    }

    /**
     * Verify if a provided PIN matches the stored PIN for an account
     * 
     * @param pinRequest Request containing account ID and PIN to verify
     * @return true if PIN is correct, false otherwise
     * @throws RuntimeException if PIN not set for the account
     */
    public boolean verifyPin(PinRequest pinRequest) {
        // Find card details with PIN validation
        CardDetails cardDetails = findCardDetailsWithPin(pinRequest.getAccountId());
        
        // Process PIN securely
        char[] pinChars = pinRequest.getPin();
        String pin = extractPinAndClear(pinChars);
        
        // Verify against stored hash
        return pinHashUtil.verifyPin(pin, cardDetails.getHashedPin());
    }

    /**
     * Change the PIN for an account
     * 
     * @param pinRequest Request containing account ID and new PIN
     * @throws IllegalArgumentException if new PIN format is invalid
     * @throws ResourceNotFoundException if account not found
     */
    public void changePin(PinRequest pinRequest) {
        // Get the new PIN and validate it
        char[] newPinChars = pinRequest.getNewPin();
        validatePinFormat(newPinChars);
        
        // Find account
        Account account = findAccount(pinRequest.getAccountId());
        
        // Process PIN securely
        String newPin = extractPinAndClear(newPinChars);
        
        // Find existing card details or create new one
        CardDetails cardDetails;
        try {
            // Try to find existing card details
            cardDetails = findCardDetailsWithPin(pinRequest.getAccountId());
        } catch (ResourceNotFoundException e) {
            // Create new card details if none exist
            cardDetails = new CardDetails(account, "");
        }
        
        // Update PIN hash and timestamp
        updateCardDetails(cardDetails, newPin);
    }
    
    /**
     * Validate that a PIN has the correct format (4 digits)
     * 
     * @param pinChars PIN character array to validate
     * @throws IllegalArgumentException if PIN format is invalid
     */
    private void validatePinFormat(char[] pinChars) {
        if (pinChars == null || pinChars.length != PIN_LENGTH) {
            throw new IllegalArgumentException("PIN must be exactly " + PIN_LENGTH + " digits");
        }
    }
    
    /**
     * Find an account by ID
     * 
     * @param accountId ID of the account to find
     * @return Account object
     * @throws RuntimeException if account not found
     */
    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
    
    /**
     * Find card details by account ID
     * 
     * @param accountId ID of the account
     * @return Optional containing card details if found
     */
    private Optional<CardDetails> findCardDetails(Long accountId) {
        return cardDetailsRepository.findByAccountId(accountId);
    }
    
    /**
     * Find card details for an account
     * 
     * @param accountId ID of the account
     * @return CardDetails object
     * @throws ResourceNotFoundException if card details not found
     */
    private CardDetails findCardDetailsWithPin(Long accountId) {
        return findCardDetails(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));
    }
    
    /**
     * Remove any existing card details for an account
     * 
     * @param accountId ID of the account
     */
    private void removeExistingCardDetails(Long accountId) {
        cardDetailsRepository.deleteByAccountId(accountId);
    }
    
    /**
     * Extract PIN string from character array and clear the array
     * 
     * @param pinChars PIN character array
     * @return PIN as string
     */
    private String extractPinAndClear(char[] pinChars) {
        String pin = new String(pinChars);
        // Security: clear the character array to remove sensitive data from memory
        Arrays.fill(pinChars, PIN_MASK_CHAR);
        return pin;
    }
    
    /**
     * Save new card details for an account
     * 
     * @param account Account to create card details for
     * @param hashedPin Hashed PIN to store
     */
    private void saveNewCardDetails(Account account, String hashedPin) {
        CardDetails cardDetails = new CardDetails(account, hashedPin);
        cardDetails.setPinCreated(true);
        cardDetails.setLastPinChanged(LocalDateTime.now());
        cardDetailsRepository.save(cardDetails);
    }
    
    
    /**
     * Update card details with new PIN
     * 
     * @param cardDetails CardDetails to update
     * @param newPin New PIN to hash and store
     */
    private void updateCardDetails(CardDetails cardDetails, String newPin) {
        String newHashedPin = pinHashUtil.hashPin(newPin);
        cardDetails.setHashedPin(newHashedPin);
        cardDetails.setLastPinChanged(LocalDateTime.now());
        cardDetailsRepository.save(cardDetails);
    }
}
