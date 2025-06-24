package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.service.AtmService;
import com.bankapp.model.AtmOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM", description = "Endpoints for ATM operations: deposit, withdraw, balance, PIN status")
public class AtmOperationController {

    @Autowired private AtmService atmService;

    // common endpoint for both deposit and withdrawal; uses authenticated user's accountId from JWT principal
    @Operation(summary = "Perform ATM operation (deposit/withdraw)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operation successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or PIN"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/operation")
    public ResponseEntity<?> performOperation(
            @AuthenticationPrincipal(expression = "accountId") Long accountId,
            @RequestBody AtmRequest request) {
        atmService.performAtmOperation(accountId, request.getAmount(), request.getPin(), request.getOperationType());
        return ResponseEntity.ok("ATM operation successful");
    }

    // returns current balance of the authenticated user's account
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            @AuthenticationPrincipal(expression = "accountId") Long accountId) {
        return atmService.getBalance(accountId);
    }

    // returns PIN status (whether a PIN is set) for the authenticated user's account
    @GetMapping("/pinStatus")
    public ResponseEntity<?> getPinStatus(
            @AuthenticationPrincipal(expression = "accountId") Long accountId) {
        return atmService.getPinStatus(accountId);
    }
} 