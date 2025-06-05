package com.bankapp.controller;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.bankapp.dto.AccountDTO;
import com.bankapp.dto.UpdateLimitsRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private static final List<String> VALID_TYPES = Arrays.asList("CHECKING", "SAVINGS");

    // Helper mapper method to convert Account entity to DTO
    private AccountDTO mapToDTO(Account account) {
        AccountDTO dto = new AccountDTO();
        dto.setId(account.getId());
        dto.setUserId(account.getUser().getId());
        dto.setType(account.getType());
        dto.setBalance(account.getBalance());
        dto.setApproved(account.isApproved());
        dto.setClosed(account.isClosed());
        dto.setDailyLimit(account.getDailyLimit());
        dto.setAbsoluteLimit(account.getAbsoluteLimit());
        return dto;
    }

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

        List<AccountDTO> accounts = accountRepository.findByUserId(userId).stream()
                .filter(a -> !a.isClosed())
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{accountId}")
    public ResponseEntity<?> updateAccount(@PathVariable Long accountId,
            @RequestParam(required = false) String type,
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
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
            }

            Optional<User> userOpt = userRepository.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User user = userOpt.get();

            if (user.getRole() == null || !"employee".equalsIgnoreCase(user.getRole())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Access denied");
            }

            List<AccountDTO> pending = accountRepository.findByApprovedFalse().stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
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

    @GetMapping("/approved")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AccountDTO>> getApprovedAccounts() {
        List<AccountDTO> accounts = accountRepository.findByApprovedTrue().stream()
                .filter(account -> !account.isClosed())
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(accounts);
    }

    @PutMapping("/{id}/limits")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> updateLimits(@PathVariable Long id, @RequestBody UpdateLimitsRequest request) {
        Optional<Account> optionalAccount = accountRepository.findById(id);
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Account account = optionalAccount.get();
        account.setDailyLimit(request.dailyLimit);
        account.setAbsoluteLimit(request.absoluteLimit);
        accountRepository.save(account);

        return ResponseEntity.ok("Limits updated successfully");
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> closeAccount(@PathVariable Long id) {
        Optional<Account> optional = accountRepository.findById(id);
        if (optional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Account account = optional.get();
        if (account.isClosed()) {
            return ResponseEntity.badRequest().body("Account already closed");
        }

        account.setClosed(true);
        accountRepository.save(account);
        return ResponseEntity.ok("Account successfully closed");
    }
}
