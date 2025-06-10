package com.bankapp.controller;

import java.math.BigDecimal;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "Endpoints for transferring money and retrieving transaction history")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    // Retrieves authenticated user from JWT authentication
    private Optional<User> getAuthenticatedUser(Authentication auth) {
        return userRepository.findByEmail(auth.getName());
    }  

    // POST endpoint to process transfers between accounts or IBANs
    @Operation(summary = "Transfer money between accounts or IBANs")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
       @ApiResponse(responseCode = "400", description = "Bad request due to invalid input"),
       @ApiResponse(responseCode = "500", description = "Internal server error during transfer")
    })
    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest transferRequest) {
          transactionService.processTransfer(transferRequest);
    return ResponseEntity.ok("Transfer completed successfully");
    }

    // Get transactions for an account with optional date and amount filters
    @Operation(summary = "Get all transactions for an account by account ID")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Returns transaction history for account"),
       @ApiResponse(responseCode = "500", description = "Error retrieving transaction history")
    })
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionHistory(
            @PathVariable Long accountId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        try {
            // Get transactions with filters
            List<TransactionHistoryDTO> transactions = transactionService.getAccountTransactionHistory(
                accountId, startDate, endDate, minAmount, maxAmount);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving transaction history: " + e.getMessage());
        }
    }

    // Get transactions by IBAN with filters (requires authentication)
    @Operation(summary = "Get transactions for an account by IBAN (authorized user only)")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Returns transaction history for IBAN"),
       @ApiResponse(responseCode = "401", description = "Unauthorized access"),
       @ApiResponse(responseCode = "403", description = "Access forbidden"),
       @ApiResponse(responseCode = "500", description = "Error retrieving transaction history")
    })
    @GetMapping("/account")
    public ResponseEntity<?> getTransactionHistoryByIban(
            @RequestParam String iban, 
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {
        try {
            // Get authenticated user
            Optional<User> userOpt = getAuthenticatedUser(authentication);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // Get transactions with authentication check
            User user = userOpt.get();
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionHistoryByIbanWithAuth(
                iban, user, startDate, endDate, minAmount, maxAmount);
            return ResponseEntity.ok(transactions);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error retrieving transaction history: " + e.getMessage());
        }
    }

    // Get all user transactions with filters (requires authentication)
    @Operation(summary = "Get all transactions made by a specific user (with authorization)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Returns transactions by user ID"),
        @ApiResponse(responseCode = "401", description = "Unauthorized access"),
        @ApiResponse(responseCode = "403", description = "Access forbidden"),
        @ApiResponse(responseCode = "500", description = "Error retrieving user transactions")
    })
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getTransactionsByUser(
            @PathVariable Long userId, 
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {
        try {
            // Get authenticated user
            Optional<User> userOpt = getAuthenticatedUser(authentication);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // Get transactions with authorization check
            User authUser = userOpt.get();
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionsByUserWithAuth(
                userId, authUser, startDate, endDate, minAmount, maxAmount);
            return ResponseEntity.ok(transactions);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error retrieving user transactions: " + e.getMessage());
        }
    }
}
