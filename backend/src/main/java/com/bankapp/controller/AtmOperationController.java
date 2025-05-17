package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
@CrossOrigin(origins = "*")
public class AtmOperationController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;

    @PostMapping("/deposit")
    public ResponseEntity<String> deposit(@RequestBody AtmRequest atmRequest) {
        return performAtmOperation(atmRequest, "DEPOSIT");
    }

    @PostMapping("/withdraw")
    public ResponseEntity<String> withdraw(@RequestBody AtmRequest atmRequest) {
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

    private ResponseEntity<String> performAtmOperation(AtmRequest atmRequest, String operationType) {
        // Validate request
        if (atmRequest == null) {
            return ResponseEntity.badRequest().body("Invalid request: Request body cannot be null");
        }
        
        if (atmRequest.getAccountId() == null) {
            return ResponseEntity.badRequest().body("Invalid request: Account ID cannot be null");
        }
        
        if (atmRequest.getAmount() == null || atmRequest.getAmount().compareTo(java.math.BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Invalid request: Amount must be greater than zero");
        }

        // Process the request
        Optional<Account> accountOpt = accountRepository.findById(atmRequest.getAccountId());
        if (accountOpt.isEmpty()) return ResponseEntity.badRequest().body("Account not found");

        Account account = accountOpt.get();
        if (operationType.equals("WITHDRAW") && account.getBalance().compareTo(atmRequest.getAmount()) < 0) {
            return ResponseEntity.badRequest().body("Insufficient balance");
        }

        if (operationType.equals("DEPOSIT")) {
            account.setBalance(account.getBalance().add(atmRequest.getAmount()));
        } else {
            account.setBalance(account.getBalance().subtract(atmRequest.getAmount()));
        }

        accountRepository.save(account);

        AtmOperation atmOperation = new AtmOperation();
        atmOperation.setAccount(account);
        atmOperation.setAmount(atmRequest.getAmount());
        atmOperation.setOperationType(
            operationType.equals("DEPOSIT") ? AtmOperation.OperationType.DEPOSIT : AtmOperation.OperationType.WITHDRAW
        );
        atmOperationRepository.save(atmOperation);

        return ResponseEntity.ok("ATM Operation successful");
    }
}