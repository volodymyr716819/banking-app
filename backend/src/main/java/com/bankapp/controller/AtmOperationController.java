package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.service.AtmService;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM", description = "Endpoints for ATM operations: deposit, withdraw, balance, PIN status")
public class AtmOperationController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private AtmService atmService;


    @Autowired
    private PinHashUtil pinHashUtil;

    /**
     * Endpoint to deposit money into an account via ATM.
     * @param atmRequest contains account ID, amount to deposit, and PIN
    */ 
   @Operation(summary = "Deposit money into an account via ATM")
   @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Deposit successful"),
       @ApiResponse(responseCode = "400", description = "Invalid request or PIN"),
       @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmRequest atmRequest) {
        atmService.processDeposit(
        atmRequest.getAccountId(),
        atmRequest.getAmount(),
        atmRequest.getPin()
    );
    return ResponseEntity.ok("Deposit successful");
    }

    /**
     * Endpoint to withdraw money from an account via ATM.
     * @param atmRequest contains account ID, amount to withdraw, and PIN
    */
   @Operation(summary = "Withdraw money from an account via ATM")
   @ApiResponses({
   @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
       @ApiResponse(responseCode = "400", description = "Insufficient balance or invalid PIN"),
       @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmRequest atmRequest) {
        atmService.processWithdrawal(
           atmRequest.getAccountId(),
           atmRequest.getAmount(),
           atmRequest.getPin()
        );
        return ResponseEntity.ok("Withdrawal successful");
    }

    /**
     * Checks the balance of an account by account ID.
     * @param accountId the ID of the account
    */
   @Operation(summary = "Get account balance")
   @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Balance returned successfully"),
       @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long accountId) {
        return atmService.getBalance(accountId);
    }

    /**
     * Checks whether a PIN has been created for an account.
     * @param accountId the ID of the account
    */
    @Operation(summary = "Check if PIN is set for account")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "PIN status returned"),
       @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @GetMapping("/pinStatus")
    public ResponseEntity<?> getPinStatus(@RequestParam Long accountId) {
        return atmService.getPinStatus(accountId);
    }
}// test for merge