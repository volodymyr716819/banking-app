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

// Service for handling bank card PIN operations including creation, verification, and changing
@Service
public class PinService {

    // Repositories and utilities
    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private PinHashUtil pinHashUtil;
    
    // PIN configuration constants
    private static final int PIN_LENGTH = 4;
    private static final char PIN_MASK_CHAR = '0';
    
    // Checks if a PIN has been created for the given account
    public boolean checkPinStatus(Long accountId) {
        return findCardDetails(accountId)
                .map(CardDetails::isPinCreated)
                .orElse(false);
    }

    // Creates a new PIN for an account
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

    // Verifies if a provided PIN matches the stored PIN for an account
    public boolean verifyPin(PinRequest pinRequest) {
        // Find card details with PIN validation
        CardDetails cardDetails = findCardDetailsWithPin(pinRequest.getAccountId());
        
        // Process PIN securely
        char[] pinChars = pinRequest.getPin();
        String pin = extractPinAndClear(pinChars);
        
        // Verify against stored hash
        return pinHashUtil.verifyPin(pin, cardDetails.getHashedPin());
    }

    // Changes the PIN for an existing account
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
    
    // Validates that a PIN has the correct format (4 digits)
    private void validatePinFormat(char[] pinChars) {
        if (pinChars == null || pinChars.length != PIN_LENGTH) {
            throw new IllegalArgumentException("PIN must be exactly " + PIN_LENGTH + " digits");
        }
    }
    
    // Finds an account by ID or throws exception if not found
    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
    }
    
    // Finds card details by account ID
    private Optional<CardDetails> findCardDetails(Long accountId) {
        return cardDetailsRepository.findByAccountId(accountId);
    }
    
    // Finds card details for an account with PIN validation
    private CardDetails findCardDetailsWithPin(Long accountId) {
        return findCardDetails(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("CardDetails", "accountId", accountId));
    }
    
    // Removes any existing card details for an account
    private void removeExistingCardDetails(Long accountId) {
        cardDetailsRepository.deleteByAccountId(accountId);
    }
    
    // Extracts PIN string from character array and clears the array for security
    private String extractPinAndClear(char[] pinChars) {
        String pin = new String(pinChars);
        // Security: clear the character array to remove sensitive data from memory
        Arrays.fill(pinChars, PIN_MASK_CHAR);
        return pin;
    }
    
    // Saves new card details for an account with hashed PIN
    private void saveNewCardDetails(Account account, String hashedPin) {
        CardDetails cardDetails = new CardDetails(account, hashedPin);
        cardDetails.setPinCreated(true);
        cardDetails.setLastPinChanged(LocalDateTime.now());
        cardDetailsRepository.save(cardDetails);
    }
    
    
    // Updates existing card details with a new PIN hash
    private void updateCardDetails(CardDetails cardDetails, String newPin) {
        String newHashedPin = pinHashUtil.hashPin(newPin);
        cardDetails.setHashedPin(newHashedPin);
        cardDetails.setLastPinChanged(LocalDateTime.now());
        cardDetailsRepository.save(cardDetails);
    }
}
