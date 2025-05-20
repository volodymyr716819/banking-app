package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
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
    
    @Autowired
    private TransactionRepository transactionRepository;

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
        boolean isDeposit = operationType.equals("DEPOSIT");
        atmOperation.setOperationType(
            isDeposit ? AtmOperation.OperationType.DEPOSIT : AtmOperation.OperationType.WITHDRAW
        );
        atmOperationRepository.save(atmOperation);
        
        /* 
        // We're creating an AtmOperation record only to avoid duplication
        // The TransactionService.getUserTransactionHistory method now converts
        // ATM operations to TransactionHistoryDTO objects for display in history
        */

        return ResponseEntity.ok("ATM Operation successful");
    }
}