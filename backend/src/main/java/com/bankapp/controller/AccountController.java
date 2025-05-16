package com.bankapp.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;

@RestController
@RequestMapping("/api/accounts")
@CrossOrigin(origins = "http://localhost:5173")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> VALID_TYPES = Arrays.asList("CHECKING", "SAVINGS");

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(@RequestParam Long userId, @RequestParam String type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return ResponseEntity.badRequest().body("User not found");

        Account account = new Account();
        account.setUser(user);
        account.setType(type);
        account.setBalance(BigDecimal.ZERO);
        account.setApproved(false);
        accountRepository.save(account);

        return ResponseEntity.ok("Account created and pending approval");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getAccountsByUserId(@PathVariable Long userId, Authentication authentication) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }

        User currentUser = userOpt.get();
        if (!currentUser.getId().equals(userId) && !currentUser.getRole().equalsIgnoreCase("employee")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        return ResponseEntity.ok(accountRepository.findByUserId(userId));
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<?> updateAccount(@PathVariable Long accountId, @RequestParam(required = false) String type,
            @RequestParam(required = false) Boolean approved) {
        Optional<Account> accOpt = accountRepository.findById(accountId);
        if (accOpt.isEmpty())
            return ResponseEntity.badRequest().body("Account not found");

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

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingAccounts(Authentication authentication) {
        try {
            String email = authentication != null ? authentication.getName() : null;

            if (email == null) {
                System.out.println("❌ No authentication context");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                System.out.println("❌ User not found for email: " + email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User user = userOpt.get();
            System.out.println("✅ Authenticated: " + user.getEmail() + " | role: " + user.getRole());

            if (user.getRole() == null || !"employee".equalsIgnoreCase(user.getRole())) {
                System.out.println("❌ Access denied — not employee");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            List<Account> pending = accountRepository.findByApprovedFalse();
            System.out.println("🟡 Returning " + pending.size() + " pending accounts");
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    @GetMapping("/debug/pending")
    public ResponseEntity<?> debugPendingAccounts() {
        try {
            List<Account> accounts = accountRepository.findByApprovedFalse();
            System.out.println("🟡 DEBUG: Pending accounts found: " + accounts.size());

            for (Account acc : accounts) {
                System.out.println("→ Account ID: " + acc.getId() +
                        ", Approved: " + acc.isApproved() +
                        ", User: " + (acc.getUser() != null ? acc.getUser().getEmail() : "null"));
            }

            return ResponseEntity.ok(accounts);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    @PutMapping("/{accountId}/approve")
    public ResponseEntity<String> approveAccount(@PathVariable Long accountId, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).orElseThrow();
        if (!"employee".equalsIgnoreCase(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
        }

        Optional<Account> optionalAccount = accountRepository.findById(accountId);
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Account not found.");
        }

        Account account = optionalAccount.get();
        account.setApproved(true);
        accountRepository.save(account);
        return ResponseEntity.ok("Account approved successfully.");
    }
}