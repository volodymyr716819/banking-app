package com.bankapp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    // method for authentication
    private Optional<User> getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName());
    }  

    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest transferRequest) {
        try {
            if ((transferRequest.getSenderIban() != null && !transferRequest.getSenderIban().isEmpty()) &&
                (transferRequest.getReceiverIban() != null && !transferRequest.getReceiverIban().isEmpty())) {
                transactionService.transferMoneyByIban(
                        transferRequest.getSenderIban(),
                        transferRequest.getReceiverIban(),
                        transferRequest.getAmount(),
                        transferRequest.getDescription());
            } else {
                transactionService.transferMoney(
                        transferRequest.getSenderAccountId(),
                        transferRequest.getReceiverAccountId(),
                        transferRequest.getAmount(),
                        transferRequest.getDescription());
            }
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred during the transfer: " + e.getMessage());
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long accountId) {
        try {
            List<TransactionHistoryDTO> transactions = transactionService.getAccountTransactionHistory(accountId);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving transaction history: " + e.getMessage());
        }
    }

    @GetMapping("/account")
    public ResponseEntity<?> getTransactionHistoryByIban(@RequestParam String iban, Authentication authentication) {
        try {
            Optional<User> userOpt = getAuthenticatedUser(authentication);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User user = userOpt.get();
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionHistoryByIbanWithAuth(iban, user);
            return ResponseEntity.ok(transactions);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error retrieving transaction history: " + e.getMessage());
        }
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Long userId, Authentication authentication) {
        try {
            Optional<User> userOpt = getAuthenticatedUser(authentication);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            User authUser = userOpt.get();
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionsByUserWithAuth(userId, authUser);
            return ResponseEntity.ok(transactions);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error retrieving user transactions: " + e.getMessage());
        }
    }
}
