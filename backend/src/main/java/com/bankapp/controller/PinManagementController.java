package com.bankapp.controller;

import com.bankapp.dto.PinRequest;
import com.bankapp.model.Account;
import com.bankapp.repository.AccountRepository;
import com.bankapp.service.PinService;
import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST Controller for PIN management operations
 * 
 * Handles PIN creation, verification, and changing for bank accounts.
 * All endpoints require user authentication and authorization.
 */
@RestController
@RequestMapping("/api/pin")
public class PinManagementController {

    private static final Logger logger = LoggerFactory.getLogger(PinManagementController.class);

    @Autowired
    private PinService pinService;

    @Autowired
    private AccountRepository accountRepository;

    /**
     * Check if a PIN has been created for an account
     * 
     * @param accountId ID of the account to check
     * @param auth Authentication object from Spring Security
     * @return Response with PIN status
     */
    @GetMapping("/check/{accountId}")
    public ResponseEntity<?> checkPinStatus(@PathVariable Long accountId, Authentication auth) {
        // Security check: verify user is authorized to access this account
        if (!isUserAuthorized(accountId, auth)) {
            return createUnauthorizedResponse();
        }
        
        // Get PIN status from service
        boolean pinCreated = pinService.checkPinStatus(accountId);
        
        // Return status to client
        return createSuccessResponse("pinCreated", pinCreated);
    }

    /**
     * Create a new PIN for an account
     * 
     * @param request PIN creation request
     * @param auth Authentication object from Spring Security
     * @return Response indicating success or failure
     */
    @PostMapping("/create")
    public ResponseEntity<?> createPin(@RequestBody PinRequest request, Authentication auth) {
        // Security check: verify user is authorized to access this account
        if (!isUserAuthorized(request.getAccountId(), auth)) {
            return createUnauthorizedResponse();
        }
        
        try {
            // Create PIN via service
            pinService.createPin(request);
            return createMessageResponse("PIN created successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // Handle validation errors
            return createMessageResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Log unexpected errors
            logger.error("Error creating PIN", e);
            return createMessageResponse("An error occurred creating the PIN", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Verify if a PIN is correct for an account
     * 
     * @param request PIN verification request
     * @param auth Authentication object from Spring Security
     * @return Response indicating if PIN is valid
     */
    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody PinRequest request, Authentication auth) {
        // Security check: verify user is authorized to access this account
        if (!isUserAuthorized(request.getAccountId(), auth)) {
            return createUnauthorizedResponse();
        }
        
        try {
            // Verify PIN via service
            boolean isValid = pinService.verifyPin(request);
            return createSuccessResponse("valid", isValid);
        } catch (ResourceNotFoundException e) {
            // Handle case where PIN hasn't been set
            return createMessageResponse("PIN not set for this account", HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            // Log unexpected errors
            logger.error("Error verifying PIN", e);
            return createMessageResponse("An error occurred verifying the PIN", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Change the PIN for an account
     * 
     * @param request PIN change request
     * @param auth Authentication object from Spring Security
     * @return Response indicating success or failure
     */
    @PostMapping("/change")
    public ResponseEntity<?> changePin(@RequestBody PinRequest request, Authentication auth) {
        // Check if user is authorized
        if (!isUserAuthorized(request.getAccountId(), auth)) {
            return createUnauthorizedResponse();
        }
        
        try {
            // Change PIN (no verification of old PIN needed)
            pinService.changePin(request);
            return createMessageResponse("PIN changed successfully", HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // Handle format errors (like wrong PIN length)
            return createMessageResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (ResourceNotFoundException e) {
            // Handle account not found
            return createMessageResponse("Account not found", HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Log unexpected errors
            logger.error("Error changing PIN", e);
            return createMessageResponse("An error occurred changing the PIN", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    /**
     * Check if the authenticated user is authorized to access an account
     * 
     * @param accountId ID of the account to check
     * @param auth Authentication object from Spring Security
     * @return true if user is authorized, false otherwise
     */
    private boolean isUserAuthorized(Long accountId, Authentication auth) {
        // Find the account
        Optional<Account> accountOpt = accountRepository.findById(accountId);
        
        // Check if account exists and belongs to authenticated user
        return accountOpt.isPresent() && 
               accountOpt.get().getUser().getEmail().equals(auth.getName());
    }
    
    /**
     * Create a standard unauthorized response
     * 
     * @return 403 Forbidden response with message
     */
    private ResponseEntity<?> createUnauthorizedResponse() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "You are not authorized to manage this account's PIN"));
    }
    
    /**
     * Create a response with a simple key-value pair
     * 
     * @param key Response key
     * @param value Response value
     * @return 200 OK response with data
     */
    private ResponseEntity<?> createSuccessResponse(String key, Object value) {
        Map<String, Object> response = new HashMap<>();
        response.put(key, value);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create a response with a message and status
     * 
     * @param message Message to include
     * @param status HTTP status
     * @return Response with given status and message
     */
    private ResponseEntity<?> createMessageResponse(String message, HttpStatus status) {
        return ResponseEntity
                .status(status)
                .body(Map.of("message", message));
    }
}
