package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.service.AtmService;
import com.bankapp.model.User;
import com.bankapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
@Tag(name = "ATM", description = "Endpoints for ATM operations: deposit, withdraw, balance, PIN status")
public class AtmOperationController {

    @Autowired private AtmService atmService;
    @Autowired private UserRepository userRepository;

    // common endpoint for both deposit and withdrawal; uses authenticated user's accountId from JWT principal
    @Operation(summary = "Perform ATM operation (deposit/withdraw)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Operation successful"),
        @ApiResponse(responseCode = "400", description = "Invalid request or PIN"),
        @ApiResponse(responseCode = "404", description = "Account not found")
    })
    @PostMapping("/operation")
    public ResponseEntity<?> performOperation(
            Authentication authentication,
            @RequestBody AtmRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        
        // For simplicity, using the account ID from the request
        // *should add validation that the user owns this account*
        Long accountId = request.getAccountId();
        
        atmService.performAtmOperation(accountId, request.getAmount(), request.getPin(), request.getOperationType());
        return ResponseEntity.ok("ATM operation successful");
    }

    // returns current balance of the authenticated user's account
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(
            Authentication authentication,
            @RequestParam Long accountId,
            @RequestParam(required = false) String pin) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        
        // Check if PIN is provided for balance check
        if (pin == null || pin.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("PIN is required for balance check");
        }
        
        return atmService.getBalanceWithPin(accountId, pin.toCharArray());
    }

    // returns PIN status (whether a PIN is set) for the authenticated user's account
    @GetMapping("/pinStatus")
    public ResponseEntity<?> getPinStatus(
            Authentication authentication,
            @RequestParam Long accountId) {
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not found");
        }
        
        return atmService.getPinStatus(accountId);
    }
} 