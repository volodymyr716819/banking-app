package com.bankapp.controller;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.dto.CreateAccountRequest;
import com.bankapp.security.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    @Autowired
    private AccountRepository accountRepository;

    @PostMapping("/create")
    public ResponseEntity<?> createAccount(
            @RequestBody CreateAccountRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();

        Account account = new Account();
        account.setUser(user);
        account.setType(request.getType());
        account.setBalance(BigDecimal.ZERO);
        account.setApproved(false); // force unapproved
        accountRepository.save(account);

        return ResponseEntity.ok("Account created and pending approval");
    }

    @GetMapping("/user")
    public ResponseEntity<List<Account>> getAccountsForCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        User user = userDetails.getUser();
        return ResponseEntity.ok(accountRepository.findByUserId(user.getId()));
    }
}