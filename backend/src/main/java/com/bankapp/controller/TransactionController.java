package com.bankapp.controller;

import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Transaction;
import com.bankapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "http://localhost:5173")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestBody TransferRequest transferRequest) {
        try {
            transactionService.transferMoney(
                transferRequest.getSenderAccountId(),
                transferRequest.getReceiverAccountId(),
                transferRequest.getAmount(),
                transferRequest.getDescription()
            );
            return ResponseEntity.ok("Transfer completed successfully");
        } catch (IllegalArgumentException ex) {
            // Return the detailed error message with 400 Bad Request status
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            // For any other errors
            return ResponseEntity.internalServerError().body("An unexpected error occurred: " + ex.getMessage());
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getTransactionHistory(@PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getAccountHistory(accountId));
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserTransactions(@PathVariable Long userId, 
                                                @RequestParam(required = false) String accountType) {
        return ResponseEntity.ok(transactionService.getUserTransactions(userId, accountType));
    }
}