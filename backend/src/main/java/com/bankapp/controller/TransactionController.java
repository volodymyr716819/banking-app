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

    
    @Operation(summary = "Get filtered transaction history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "User not authenticated"),
        @ApiResponse(responseCode = "403", description = "Access denied - insufficient permissions"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/history")
    public ResponseEntity<?> getTransactionHistory(
        @ModelAttribute TransactionFilterRequest filters,// Spring automatically fills this from URL params
        Authentication authentication) {
        
        try {
            // Find the logged-in user
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
           // Customer tried to view someone else's transactions
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
           // database errors,or another problenm 
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
           
            transactionService.processTransfer(transferRequest);
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException e) {
           
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Catches unexpected errors like database failures or network issues
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error processing transfer: " + e.getMessage());
        }
    }
}