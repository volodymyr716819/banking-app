package com.bankapp.controller;

import com.bankapp.dto.PinRequest;
import com.bankapp.model.Account;
import com.bankapp.model.CardDetails;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pin")
public class PinManagementController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    
    @Autowired
    private PinHashUtil pinHashUtil;

    /**
     * Check if a PIN has been created for the account
     */
    @GetMapping("/check/{accountId}")
    public ResponseEntity<?> checkPinStatus(@PathVariable Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("pinCreated", cardDetailsOpt.isPresent() && cardDetailsOpt.get().isPinCreated());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Create or update a PIN for an account
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPin(@RequestBody PinRequest pinRequest) {
        if (pinRequest.getPin() == null || pinRequest.getPin().length() != 4) {
            return ResponseEntity.badRequest().body("PIN must be 4 digits");
        }

        Optional<Account> accountOpt = accountRepository.findById(pinRequest.getAccountId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Account account = accountOpt.get();
        
        // Hash the PIN before storing
        String hashedPin = pinHashUtil.hashPin(pinRequest.getPin());
        
        // Check if card details already exist
        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(account.getId());
        CardDetails cardDetails;
        
        if (cardDetailsOpt.isPresent()) {
            // Update existing card details
            cardDetails = cardDetailsOpt.get();
            cardDetails.setHashedPin(hashedPin);
            cardDetails.setPinCreated(true);
            cardDetails.setLastPinChanged(LocalDateTime.now());
        } else {
            // Create new card details
            cardDetails = new CardDetails(account, hashedPin);
        }
        
        cardDetailsRepository.save(cardDetails);
        
        return ResponseEntity.ok("PIN created successfully");
    }

    /**
     * Verify a PIN for an account
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody PinRequest pinRequest) {
        Optional<Account> accountOpt = accountRepository.findById(pinRequest.getAccountId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(pinRequest.getAccountId());
        if (cardDetailsOpt.isEmpty() || !cardDetailsOpt.get().isPinCreated()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIN not set for this account");
        }

        CardDetails cardDetails = cardDetailsOpt.get();
        boolean isValid = pinHashUtil.verifyPin(pinRequest.getPin(), cardDetails.getHashedPin());
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Change a PIN for an account
     */
    @PostMapping("/change")
    public ResponseEntity<?> changePin(@RequestBody PinRequest pinRequest) {
        if (pinRequest.getNewPin() == null || pinRequest.getNewPin().length() != 4) {
            return ResponseEntity.badRequest().body("New PIN must be 4 digits");
        }

        Optional<Account> accountOpt = accountRepository.findById(pinRequest.getAccountId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found");
        }

        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(pinRequest.getAccountId());
        if (cardDetailsOpt.isEmpty() || !cardDetailsOpt.get().isPinCreated()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIN not set for this account");
        }

        CardDetails cardDetails = cardDetailsOpt.get();
        
        // Verify current PIN
        if (!pinHashUtil.verifyPin(pinRequest.getPin(), cardDetails.getHashedPin())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Current PIN is incorrect");
        }
        
        // Hash and update the new PIN
        String newHashedPin = pinHashUtil.hashPin(pinRequest.getNewPin());
        cardDetails.setHashedPin(newHashedPin);
        cardDetails.setLastPinChanged(LocalDateTime.now());
        cardDetailsRepository.save(cardDetails);
        
        return ResponseEntity.ok("PIN changed successfully");
    }
}