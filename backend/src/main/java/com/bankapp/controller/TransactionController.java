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

    // Retrieves authenticated user from JWT authentication
    private Optional<User> getAuthenticatedUser(Authentication auth) {
        // Get user by email from the authentication object
        return userRepository.findByEmail(auth.getName());
    }  

    // POST endpoint to process transfers between accounts or IBANs
    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest transferRequest) {
        try {
            // Process the transfer
            transactionService.processTransfer(transferRequest);
            
            // Return success message
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return handleTransactionError(e, "An error occurred during the transfer");
        }
    }

    // GET endpoint to fetch all transactions for a given account ID
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long accountId) {
        try {
            // Get account transactions
            List<TransactionHistoryDTO> transactions = 
                transactionService.getAccountTransactionHistory(accountId);
            
            // Return successful response
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            // Handle unexpected errors
            return handleTransactionError(e, "Error retrieving transaction history");
        }
    }

    // GET endpoint to fetch transactions by IBAN, restricted to account owner or employee
    @GetMapping("/account")
    public ResponseEntity<?> getTransactionHistoryByIban(@RequestParam String iban, Authentication authentication) {
        try {
            // Get authenticated user
            User user = validateAndGetUser(authentication);
            
            // Get transactions with authorization check
            List<TransactionHistoryDTO> transactions = transactionService
                .getTransactionHistoryByIbanWithAuth(iban, user);
            
            // Return successful response
            return ResponseEntity.ok(transactions);
            
        } catch (IllegalArgumentException e) {
            // Handle permission errors
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return handleTransactionError(e, "Error retrieving transaction history");
        }
    }
    
    // Validate and get the authenticated user
    private User validateAndGetUser(Authentication authentication) {
        Optional<User> userOpt = getAuthenticatedUser(authentication);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        
        return userOpt.get();
    }
    
    // Handle transaction errors and return appropriate response
    private ResponseEntity<?> handleTransactionError(Exception e, String message) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(message + ": " + e.getMessage());
    }

    // GET endpoint to fetch all transactions made by a user (by userId), with authorization check
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(@PathVariable Long userId, Authentication authentication) {
        try {
            // Get authenticated user
            User authUser = validateAndGetUser(authentication);
            
            // Get user transactions with authorization check
            List<TransactionHistoryDTO> transactions = 
                transactionService.getTransactionsByUserWithAuth(userId, authUser);
            
            // Return successful response
            return ResponseEntity.ok(transactions);
            
        } catch (IllegalArgumentException e) {
            // Handle permission errors
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            // Handle unexpected errors
            return handleTransactionError(e, "Error retrieving user transactions");
        }
    }
}
