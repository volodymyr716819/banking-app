package com.bankapp.controller;

import com.bankapp.dto.PinRequest;
import com.bankapp.model.Account;
import com.bankapp.repository.AccountRepository;
import com.bankapp.service.PinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/pin")
@Tag(name = "PIN Management", description = "Endpoints for managing PINs: create, verify, change")
public class PinManagementController {

    @Autowired
    private PinService pinService;

    @Autowired
    private AccountRepository accountRepository;

    // Check if authenticated user owns the account
    private boolean isAuthorizedUser(Long accountId, Authentication auth) {
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        return accountOpt.isPresent()
                && accountOpt.get().getUser().getEmail().equals(auth.getName());
    }

    @Operation(summary = "Check if a PIN has been set for the account")
    @ApiResponses({  
       @ApiResponse(responseCode = "200", description = "Returns whether a PIN is created"),
       @ApiResponse(responseCode = "403", description = "Unauthorized access to account")
    })
    @GetMapping("/check/{accountId}")
    public ResponseEntity<?> checkPinStatus(@PathVariable Long accountId, Authentication auth) {
        if (!isAuthorizedUser(accountId, auth)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }
        boolean status = pinService.checkPinStatus(accountId);
        return ResponseEntity.ok(Map.of("pinCreated", status));
    }

    @Operation(summary = "Create a new PIN for the account")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "PIN created successfully"),
       @ApiResponse(responseCode = "403", description = "Unauthorized access to account")
    })
    @PostMapping("/create")
    public ResponseEntity<?> createPin(@RequestBody PinRequest request, Authentication auth) {
        if (!isAuthorizedUser(request.getAccountId(), auth)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }
        pinService.createPin(request);
        return ResponseEntity.ok("PIN created successfully");
    }

    @Operation(summary = "Verify entered PIN for the account")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Returns whether the PIN is valid"),
       @ApiResponse(responseCode = "403", description = "Unauthorized access to account")
    })
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody PinRequest request, Authentication auth) {
        if (!isAuthorizedUser(request.getAccountId(), auth)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }
        boolean isValid = pinService.verifyPin(request);
        return ResponseEntity.ok(Map.of("valid", isValid));
    }

    @Operation(summary = "Change the existing PIN for the account")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "PIN changed successfully"),
        @ApiResponse(responseCode = "403", description = "Unauthorized access to account")
    })
    @PostMapping("/change")
    public ResponseEntity<?> changePin(@RequestBody PinRequest request, Authentication auth) {
        if (!isAuthorizedUser(request.getAccountId(), auth)) {
            return ResponseEntity.status(403).body(Map.of("message", "Unauthorized"));
        }
        pinService.changePin(request);
        return ResponseEntity.ok("PIN changed successfully");
    }
}
