package com.bankapp.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.bankapp.model.User;
import java.util.List;
import com.bankapp.model.ATMRequest;
import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.model.TransactionType;
import com.bankapp.repository.UserRepository;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/atm")
@CrossOrigin(origins = "*")
public class ATMController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired 
    private UserRepository userRepository;

    // Deposit
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody ATMRequest request) {
        return handleTransaction(request, TransactionType.DEPOSIT);
    }

    // Withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody ATMRequest request) {
        return handleTransaction(request, TransactionType.WITHDRAWAL);
    }

    // Get balance (called in Vue)
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long userId) {
        Account account = accountRepository.findByUserId(userId)
            .stream().findFirst().orElse(null);
        if (account == null) {
            return ResponseEntity.status(404).body("No account found for this user");
        }
        return ResponseEntity.ok(account.getBalance());
    }

    private ResponseEntity<?> handleTransaction(ATMRequest req, TransactionType type) {
        if (req.getUserId() == null || req.getAmount() == null) {
            return ResponseEntity.badRequest().body("Missing input");
        }

        Account account = accountRepository.findByUserId(req.getUserId())
            .stream().findFirst().orElse(null);
        if (account == null || !account.isApproved()) {
            return ResponseEntity.badRequest().body("Account not found or not approved");
        }

        if (type == TransactionType.WITHDRAWAL &&
                account.getBalance().compareTo(req.getAmount()) < 0) {
            return ResponseEntity.badRequest().body("Insufficient funds");
        }

        BigDecimal newBalance = type == TransactionType.DEPOSIT
            ? account.getBalance().add(req.getAmount())
            : account.getBalance().subtract(req.getAmount());

        account.setBalance(newBalance);
        accountRepository.save(account);

        Transaction tx = new Transaction();
        tx.setAccount(account);
        tx.setAmount(req.getAmount());
        tx.setType(type);
        tx.setTimestamp(LocalDateTime.now());
        transactionRepository.save(tx);

        return ResponseEntity.ok("Transaction successful");
    }
}