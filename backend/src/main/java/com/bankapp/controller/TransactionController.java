package com.bankapp.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.User;
import com.bankapp.service.TransactionService;

@RestController
@RequestMapping("/api/transactions")
@Tag(name = "Transaction", description = "Endpoints for transferring money and retrieving transaction history")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    // ------------------------ Transfer Endpoint ------------------------

    @PostMapping("/transfer")
    @Operation(summary = "Transfer money between accounts using IBANs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transfer completed successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid transfer data"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> transferMoney(@RequestBody TransferRequest transferRequest) {
        try {
            transactionService.processTransfer(transferRequest);
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    // ------------------------ Public Transaction History by IBAN ------------------------

    @GetMapping("/account/{iban}")
    @Operation(summary = "Get transactions for an account (by IBAN)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction history returned"),
        @ApiResponse(responseCode = "500", description = "Server error retrieving transaction history")
    })
    public ResponseEntity<?> getTransactionHistoryPublic(
            @PathVariable String iban,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount) {
        try {
            List<TransactionHistoryDTO> transactions = transactionService.getAccountTransactionHistoryByIban(
                    iban, startDate, endDate, minAmount, maxAmount);
            return ResponseEntity.ok(transactions);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving transaction history: " + e.getMessage());
        }
    }

    // ------------------------ Authenticated Transaction History by IBAN ------------------------

    @GetMapping("/account")
    @Operation(summary = "Get account transactions by IBAN (authorized access)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction history returned"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Server error retrieving transaction history")
    })
    public ResponseEntity<?> getTransactionHistoryByIbanWithAuth(
            @RequestParam String iban,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {

        try {
            User user = getAuthenticatedUser(authentication);
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

    // ------------------------ Authenticated Transaction History by User ------------------------

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user transactions (authorized access)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Transaction history returned"),
        @ApiResponse(responseCode = "403", description = "Access denied"),
        @ApiResponse(responseCode = "500", description = "Server error retrieving transaction history")
    })
    public ResponseEntity<?> getTransactionsByUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            Authentication authentication) {

        try {
            User user = getAuthenticatedUser(authentication);
            List<TransactionHistoryDTO> transactions = transactionService.getTransactionsByUserWithAuth(
                    userId, user, startDate, endDate, minAmount, maxAmount);
            return ResponseEntity.ok(transactions);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error retrieving user transactions: " + e.getMessage());
        }
    }

    // ------------------------ Helper ------------------------

    private User getAuthenticatedUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof User user) {
            return user;
        }
        throw new IllegalStateException("Authentication principal is not a User instance");
    }
}
