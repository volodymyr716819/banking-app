package com.bankapp.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.bankapp.dto.UserDTO;
import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.exception.UserNotFoundException;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.AccountService;
import com.bankapp.service.UserService;
import com.bankapp.service.UserSearchService;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User Management", description = "Endpoints for managing users and registration flow")
public class UserController {

    private final UserRepository userRepository;
    private final UserSearchService userSearchService;
    private final AccountService accountService;
    private final UserService userService;
    
    public UserController(
        UserRepository userRepository,
        UserSearchService userSearchService,
        AccountService accountService,
        UserService userService
    ) {
        this.userRepository = userRepository;
        this.userSearchService = userSearchService;
        this.accountService = accountService;
        this.userService = userService;
    }

    private UserDTO mapToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRole());
        dto.setApproved(user.isApproved());
        return dto;
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
    public UserDTO getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        return mapToDTO(user);
    }

    @Operation(summary = "Delete a user by ID")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "User deleted"),
       @ApiResponse(responseCode = "404", description = "User not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException("User not found with id: " + id);
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted.");
    }

    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Get users pending approval (EMPLOYEE only)")
    @ApiResponse(responseCode = "200", description = "Returns list of pending users with full details")
    @GetMapping("/pending")
    public List<User> getPendingUsers() {
        // Return full user information for pending users
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

        @GetMapping("/search")
    public List<UserSearchResultDTO> searchCustomersByName(
        @RequestParam String name,
        Authentication authentication) {
        return userSearchService.searchUsersByName(name, authentication);
    }
    
    @PreAuthorize("hasRole('EMPLOYEE')")
    @Operation(summary = "Update user information (EMPLOYEE only)")
    @ApiResponses({
       @ApiResponse(responseCode = "200", description = "User updated successfully"),
       @ApiResponse(responseCode = "400", description = "Update failed"),
       @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> updates) {
        try {
            User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
            
            if (updates.containsKey("name")) user.setName(updates.get("name"));
            if (updates.containsKey("email")) user.setEmail(updates.get("email"));
            
            userRepository.save(user);
            return ResponseEntity.ok("User updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}