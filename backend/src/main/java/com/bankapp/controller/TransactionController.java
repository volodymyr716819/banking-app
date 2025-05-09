package com.bankapp.controller;

import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Transaction;
import com.bankapp.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<String> transferMoney(@RequestBody TransferRequest transferRequest) {
        transactionService.transferMoney(
            transferRequest.getSenderAccountId(),
            transferRequest.getReceiverAccountId(),
            transferRequest.getAmount(),
            transferRequest.getDescription()
        );
        return ResponseEntity.ok("Transfer completed successfully");
    }
}