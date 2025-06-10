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

import com.bankapp.dto.UserDTO;
import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.UserService;
import com.bankapp.service.UserSearchService;

@RestController
@RequestMapping("/api/users")
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

    @GetMapping
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        return userOpt.map(user -> ResponseEntity.ok(mapToDTO(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        userRepository.deleteById(id);
        return ResponseEntity.ok("User deleted.");
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<User> getPendingUsers() {
        return userRepository.findByRegistrationStatus(RegistrationStatus.PENDING);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> approveUser(@PathVariable Long id) {
        try {
            userService.approveUser(id);
            return ResponseEntity.ok("User approved successfully.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/approved")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<User> getApprovedCustomers() {
        return userRepository.findByRegistrationStatusAndRoleIgnoreCase(RegistrationStatus.APPROVED, "CUSTOMER");
    }

    // Search endpoints for finding customers by name or email
    @GetMapping("/find-by-name")
    public ResponseEntity<?> searchUsersByName(@RequestParam String name, Authentication authentication) {
        return searchUsers(name, null, null, null, authentication);
    }

    @GetMapping("/find-by-email")
    public ResponseEntity<?> searchUsersByEmail(@RequestParam String email, Authentication authentication) {
        return searchUsers(null, null, email, null, authentication);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> declineUser(@PathVariable Long id) {
        try {
           userService.declineUser(id);
           return ResponseEntity.ok("User declined successfully.");
        } catch (RuntimeException e) {
           return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Search endpoint that accepts term, name, email, or IBAN parameters
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
