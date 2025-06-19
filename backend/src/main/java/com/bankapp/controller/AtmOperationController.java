package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.model.User;
import com.bankapp.model.AtmOperation.OperationType;
import com.bankapp.service.AtmService;
import com.bankapp.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM", description = "ATM operations: deposit, withdraw, balance, PIN status")
public class AtmOperationController {

    @Autowired private AtmService atmService;
    @Autowired private UserService userService;

    // Handles deposit request
    @PostMapping("/deposit")
    @Operation(summary = "Deposit into account via ATM")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deposit successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> deposit(@RequestBody AtmRequest request, Authentication auth) {
        userService.validateAuthentication(auth).orElseThrow(() ->
            new RuntimeException("Authentication failed"));

        atmService.performAtmOperation(request.getAccountId(), request.getAmount(), request.getPin(), OperationType.DEPOSIT);
        return ResponseEntity.ok("Deposit successful");
    }

    // Handles withdraw request
    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from account via ATM")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Withdrawal successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> withdraw(@RequestBody AtmRequest request, Authentication auth) {
        userService.validateAuthentication(auth).orElseThrow(() ->
            new RuntimeException("Authentication failed"));
        
        atmService.performAtmOperation(request.getAccountId(), request.getAmount(), request.getPin(), OperationType.WITHDRAW);
        return ResponseEntity.ok("Withdrawal successful");
    }

    // Returns account balance
    @GetMapping("/balance")
    @Operation(summary = "Get account balance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Balance returned"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> getBalance(@RequestParam Long accountId, Authentication auth) {
        userService.validateAuthentication(auth).orElseThrow(() ->
            new RuntimeException("Authentication failed"));
        return atmService.getBalance(accountId);
    }

    // Returns PIN set status
    @GetMapping("/pinStatus")
    @Operation(summary = "Check PIN status")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PIN status returned"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    public ResponseEntity<?> getPinStatus(@RequestParam Long accountId, Authentication auth) {
        userService.validateAuthentication(auth).orElseThrow(() ->
            new RuntimeException("Authentication failed"));
        return atmService.getPinStatus(accountId);
    }
}
