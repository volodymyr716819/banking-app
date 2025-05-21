package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
public class AtmOperationController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    @Autowired
    private CardDetailsRepository cardDetailsRepository;
    
    @Autowired
    private PinHashUtil pinHashUtil;

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmRequest atmRequest) {
        // Verify PIN first
        ResponseEntity<?> pinVerification = verifyPin(atmRequest);
        if (pinVerification != null) {
            return pinVerification;
        }
        
        return performAtmOperation(atmRequest, "DEPOSIT");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmRequest atmRequest) {
        // Verify PIN first
        ResponseEntity<?> pinVerification = verifyPin(atmRequest);
        if (pinVerification != null) {
            return pinVerification;
        }
        
        return performAtmOperation(atmRequest, "WITHDRAW");
    }

    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Account not found");
        }
        return ResponseEntity.ok(accountOpt.get().getBalance());
    }
    
    @GetMapping("/pinStatus")
    public ResponseEntity<?> getPinStatus(@RequestParam Long accountId) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return ResponseEntity.status(404).body("Account not found");
        }
        
        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(accountId);
        Map<String, Object> response = new HashMap<>();
        response.put("pinCreated", cardDetailsOpt.isPresent() && cardDetailsOpt.get().isPinCreated());
        
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<?> verifyPin(AtmRequest atmRequest) {
        if (atmRequest.getPin() == null) {
            return ResponseEntity.badRequest().body("PIN is required");
        }
        
        Optional<CardDetails> cardDetailsOpt = cardDetailsRepository.findByAccountId(atmRequest.getAccountId());
        if (cardDetailsOpt.isEmpty() || !cardDetailsOpt.get().isPinCreated()) {
            return ResponseEntity.badRequest().body("PIN not set for this account");
        }
        
        CardDetails cardDetails = cardDetailsOpt.get();
        if (!pinHashUtil.verifyPin(atmRequest.getPin(), cardDetails.getHashedPin())) {
            return ResponseEntity.status(401).body("Invalid PIN");
        }
        
        return null; // PIN verification passed
    }

    private ResponseEntity<String> performAtmOperation(AtmRequest atmRequest, String operationType) {
        Optional<Account> accountOpt = accountRepository.findById(atmRequest.getAccountId());
        if (accountOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Account not found");
        }

        Account account = accountOpt.get();
        
        // Check if account is approved
        if (!account.isApproved()) {
            return ResponseEntity.badRequest().body("Account is not approved for ATM operations");
        }
        
        // Check if account is closed
        if (account.isClosed()) {
            return ResponseEntity.badRequest().body("Account is closed and cannot perform ATM operations");
        }
        
        // Check balance for withdrawals
        if (operationType.equals("WITHDRAW") && account.getBalance().compareTo(atmRequest.getAmount()) < 0) {
            return ResponseEntity.badRequest().body("Insufficient balance");
        }

        // Process ATM operation
        if (operationType.equals("DEPOSIT")) {
            account.setBalance(account.getBalance().add(atmRequest.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(atmRequest.getAmount()));
        }

        accountRepository.save(account);

        // Record the ATM operation
        AtmOperation atmOperation = new AtmOperation();
        atmOperation.setAccount(account);
        atmOperation.setAmount(atmRequest.getAmount());
        boolean isDeposit = operationType.equals("DEPOSIT");
        atmOperation.setOperationType(
            isDeposit ? AtmOperation.OperationType.DEPOSIT : AtmOperation.OperationType.WITHDRAW
        );
        atmOperationRepository.save(atmOperation);

        return ResponseEntity.ok("ATM Operation successful");
    }
}