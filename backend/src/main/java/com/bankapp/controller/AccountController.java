package com.bankapp.controller;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:5173") 
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> VALID_TYPES = Arrays.asList("CHECKING", "SAVINGS");

    // Temporary create account for a hardcoded user (e.g., user with ID 1)
    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestParam Long userId, @RequestParam String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        Account account = new Account();
        account.setUser(user);
        account.setType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setApproved(false);
        accountRepository.save(account);

        return ResponseEntity.ok("Account created and pending approval");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Account>> getAccountsByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(accountRepository.findByUserId(userId));
    }

     @PutMapping("/{accountId}")
    public ResponseEntity<?> updateAccount(@PathVariable Long accountId, @RequestParam(required = false) String type, @RequestParam(required = false) Boolean approved) {
        Optional<Account> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty()) return ResponseEntity.badRequest().body("Account not found");

        Account acc = accOpt.get();

        if (type != null) {
            if (!VALID_TYPES.contains(type.toUpperCase())) {
                return ResponseEntity.badRequest().body("Invalid account type");
            }
            acc.setType(type.toUpperCase());
        }

        if (approved != null) {
            acc.setApproved(approved);
        }

        accountRepository.save(acc);
        return ResponseEntity.ok("Account updated");
    }
}
