package com.bankapp.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Transactions", description = "Endpoints for managing transactions and transfers")
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    
    public TransactionController(TransactionService transactionService, UserRepository userRepository) {
        this.transactionService = transactionService;
        this.userRepository = userRepository;
    }

    /**
     * Retrieves transaction history with role-based access control.
     * Employees can view all transactions across all accounts in the system.
     * Customers can only view transactions for their own accounts.
     * Accepts filter parameters like date range, account ID, and transaction type.
     */
    @Operation(summary = "Get filtered transaction history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
        @ModelAttribute TransactionFilterRequest filters,
        Authentication authentication) {
        
        try {
            // Extract user email from JWT token and look up user in database
            // The authentication.getName() method returns the email from the JWT token
            Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
            }

            // Get the authenticated user from the Optional
            User user = userOpt.get();
            
            // Call the transaction service to fetch transaction history
            // The service checks if the user is an employee or customer: 
            
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionHistory(filters, user);
            
            // Return the list of transactions as a successful HTTP response
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            // This exception is thrown when a customer tries to access transactions
            // from accounts they don't own
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            // Catches any unexpected errors like database connection issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving transactions: " + e.getMessage());
        }
    }

    /**
     * Processes a money transfer between two bank accounts.
     * This endpoint performs comprehensive validation before executing the transfer,
     * including checking account ownership, sufficient balance, and transfer limits.
     */
    @Operation(summary = "Process money transfer between accounts")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid transfer request - validation failed"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "500", description = "Internal server error during transfer")
    })
    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest transferRequest) {
        try {
            // Process the transfer request through the transaction service
            // The processTransfer method performs the following validations and actions:
            // - Verifies the source account belongs to the authenticated user
            // - Checks if the source account has sufficient balance for the transfer
            // - Validates that daily and monthly transfer limits are not exceeded
            // - Ensures both source and destination accounts exist and are active
            // - Creates debit transaction for source account
            // - Creates credit transaction for destination account
            // - Updates balances for both accounts in a single database transaction
            transactionService.processTransfer(transferRequest);
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException e) {
            // This exception is thrown when validation fails, such as:
            // - Insufficient funds in the source account
            // - Invalid account numbers (non-existent accounts)
            // - Transfer amount exceeds daily or monthly limits
            // - Attempting to transfer from an account not owned by the user
            // - Source or destination account is inactive or closed
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Catches unexpected errors like database failures or network issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing transfer: " + e.getMessage());
        }
    }
}