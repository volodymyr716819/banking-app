package com.bankapp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.bankapp.dto.TransactionFilterRequest;
import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.User;
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

    // Get transaction history for customers and employees with filtering
    @Operation(summary = "Get transaction history with filters")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Returns filtered transaction history"),
       @ApiResponse(responseCode = "401", description = "Unauthorized access"),
       @ApiResponse(responseCode = "403", description = "Access forbidden"),
       @ApiResponse(responseCode = "500", description = "Error retrieving transaction history")
    })
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
        @ModelAttribute TransactionFilterRequest filters,
        Authentication authentication) {
        try {
            // Get authenticated user
            Optional<User> userOpt = getAuthenticatedUser(authentication);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // Get transactions with filters and authorization
            User user = userOpt.get();
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionHistory(filters, user);
            return ResponseEntity.ok(transactions);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
              .body("Error retrieving transaction history: " + e.getMessage());
        }
    }
}
// test for merge