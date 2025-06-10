package com.bankapp.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.bankapp.dto.UserDTO;
import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.service.UserService;
import com.bankapp.service.UserSearchService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing users and registration flow")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSearchService userSearchService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserService userService;

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setApproved(user.isApproved());
        return dto;
    }

    private String generateIban(Account account) {
        String countryCode = "NL";
        String bankCode = "BANK";
        String paddedId = String.format("%010d", account.getId());
        return countryCode + bankCode + paddedId;
    }

    @Operation(summary = "Get all registered users")
    @ApiResponse(responseCode = "200", description = "Returns a list of all users")
    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get user by ID")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Returns the user"),
       @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(user -> ResponseEntity.ok(mapToDTO(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Operation(summary = "Delete a user by ID")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "User deleted"),
       @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted.");
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get users pending approval (EMPLOYEE only)")
    @ApiResponse(responseCode = "200", description = "Returns list of pending users")
    @GetMapping("/pending")
    public List<User> getPendingUsers() {
        return userRepository.findByRegistrationStatus(RegistrationStatus.PENDING);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Approve user by ID (EMPLOYEE only)")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "User approved"),
       @ApiResponse(responseCode = "400", description = "Approval failed")
    })
    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        try {
            userService.approveUser(id);
            return ResponseEntity.ok("User approved successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get all approved customers (EMPLOYEE only)")
    @ApiResponse(responseCode = "200", description = "Returns approved users with CUSTOMER role")
    @GetMapping("/approved")
    public List<User> getApprovedCustomers() {
        return userRepository.findByRegistrationStatusAndRoleIgnoreCase(RegistrationStatus.APPROVED, "CUSTOMER");
    }

    // Legacy search endpoints - kept for backward compatibility
    @Operation(summary = "Search users by name (legacy)")
    @ApiResponse(responseCode = "200", description = "Search results")
    @GetMapping("/find-by-name")
    public ResponseEntity<?> searchUsersByName(@RequestParam String name, Authentication authentication) {
        return searchUsers(name, null, null, null, authentication);
    }

    @Operation(summary = "Search users by email (legacy)")
    @ApiResponse(responseCode = "200", description = "Search results")
    @GetMapping("/find-by-email")
    public ResponseEntity<?> searchUsersByEmail(@RequestParam String email, Authentication authentication) {
        return searchUsers(null, null, email, null, authentication);
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Decline user registration (EMPLOYEE only)")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "User declined"),
       @ApiResponse(responseCode = "400", description = "Decline failed")
    })
    @PostMapping("/{id}/decline")
    public ResponseEntity<?> declineUser(@PathVariable Long id) {
        try {
           userService.declineUser(id);
           return ResponseEntity.ok("User declined successfully.");
        } catch (RuntimeException e) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    //  search endpoint with 1 search term: name, email, or IBAN
    @Operation(summary = "Advanced user search by term, name, email, or IBAN")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "Search results"),
       @ApiResponse(responseCode = "400", description = "Invalid search input")
    })
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
        @RequestParam(required = false) String term,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String iban,
        Authentication authentication) {
        
        try {
           return ResponseEntity.ok(userSearchService.searchUsers(term, name, email, iban, authentication));
        } catch (IllegalArgumentException ex) {
           return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
