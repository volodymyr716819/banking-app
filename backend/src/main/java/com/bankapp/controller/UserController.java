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
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;


    @Autowired
    private AccountRepository accountRepository;

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

    @Autowired
    private UserService userService;

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
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok("User approved successfully.");
    }

    @GetMapping("/approved")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public List<User> getApprovedCustomers() {
        return userRepository.findByRegistrationStatusAndRoleIgnoreCase(RegistrationStatus.APPROVED, "CUSTOMER");
    }

    /**
     * Legacy search endpoints - kept for backward compatibility
     */
    @GetMapping("/find-by-name")
    public ResponseEntity<?> searchUsersByName(@RequestParam String name, Authentication authentication) {
        return searchUsers(name, null, null, null, authentication);
    }

    @GetMapping("/find-by-email")

    @GetMapping("/find-by-email")
    public ResponseEntity<?> searchUsersByEmail(@RequestParam String email, Authentication authentication) {
        return searchUsers(null, null, email, null, authentication);
    }

    @PostMapping("/{id}/decline")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<?> declineUser(@PathVariable Long id) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        user.setRegistrationStatus(RegistrationStatus.DECLINED);
        userRepository.save(user);
        return ResponseEntity.ok("User declined successfully.");
    }

    /**
     * Simplified search endpoint with a single search term
     * Search term can be a name, email, or IBAN
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String term,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String iban,
            Authentication authentication) {

        // Check if the user is authenticated
        if (authentication == null) {
            return ResponseEntity.badRequest().body("User not authenticated");
        }

        // Get the authenticated user
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("User not found");
        }

        // Only authenticated users can search
        if (!userOpt.get().isApproved()) {
            return ResponseEntity.badRequest().body("User not approved");
        }

        // If term is provided, use it as the universal search term
        if (term != null && !term.trim().isEmpty()) {
            List<User> matchingUsers = userRepository.findApprovedCustomersBySearchTerm(term.trim());

            // Check if the search term might be an IBAN
            if (matchingUsers.isEmpty() && term.replaceAll("\\s+", "").toUpperCase().contains("BANK")) {
                String cleanTerm = term.replaceAll("\\s+", "").toUpperCase();
                if (cleanTerm.startsWith("NLBANK")) {
                    try {
                        String accountIdPart = cleanTerm.substring("NLBANK".length());
                        Long accountId = Long.parseLong(accountIdPart);


                        Optional<Account> account = accountRepository.findById(accountId);
                        if (account.isPresent() && account.get().isApproved() && !account.get().isClosed()) {
                            User accountOwner = account.get().getUser();
                            if (accountOwner.isApproved() && "CUSTOMER".equalsIgnoreCase(accountOwner.getRole())) {
                                matchingUsers = List.of(accountOwner);
                            }
                        }
                    } catch (NumberFormatException | IndexOutOfBoundsException e) {
                        // invalid IBAN format
                    }
                }
            }


            List<UserSearchResultDTO> results = matchingUsers.stream()
                    .map(user -> {
                        List<Account> accounts = accountRepository.findByUserId(user.getId());
                        List<String> ibans = accounts.stream()
                                .filter(Account::isApproved)
                                .filter(account -> !account.isClosed())
                                .map(this::generateIban)
                                .collect(Collectors.toList());
                        return new UserSearchResultDTO(user.getId(), user.getName(), ibans);
                    })
                    .collect(Collectors.toList());


            return ResponseEntity.ok(results);
        }

        // Legacy behavior for backward compatibility
        // Validate that at least one search parameter is provided
        if ((name == null || name.trim().isEmpty()) &&
                (email == null || email.trim().isEmpty()) &&
                (iban == null || iban.trim().isEmpty())) {
            return ResponseEntity.badRequest().body("At least one search parameter is required");
        }


        List<User> matchingUsers = new ArrayList<>();

        // If IBAN is provided, search by IBAN
        if (iban != null && !iban.trim().isEmpty()) {
            String cleanIban = iban.replaceAll("\\s+", "");

            // Extract the numeric part (account ID) from the IBAN format
            if (cleanIban.startsWith("NLBANK")) {
                try {
                    String accountIdPart = cleanIban.substring("NLBANK".length());
                    Long accountId = Long.parseLong(accountIdPart);

                    // Find the account by ID
                    Optional<Account> accountOpt = accountRepository.findById(accountId);
                    if (accountOpt.isPresent() && accountOpt.get().isApproved() && !accountOpt.get().isClosed()) {
                        User accountOwner = accountOpt.get().getUser();
                        if (accountOwner.isApproved() && "customer".equalsIgnoreCase(accountOwner.getRole())) {
                            matchingUsers.add(accountOwner);
                        }
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // invalid IBAN format
                }
            }
        } else {
            // Search by name and/or email
            matchingUsers = userRepository.searchApprovedByNameEmailAndRole(
                    (name != null && !name.trim().isEmpty()) ? name : null,
                    (email != null && !email.trim().isEmpty()) ? email : null,
                    "customer");
        }

        // Convert to DTOs with IBANs
        List<UserSearchResultDTO> results = matchingUsers.stream()
                .map(user -> {
                    List<Account> accounts = accountRepository.findByUserId(user.getId());
                    List<String> ibans = accounts.stream()
                            .filter(Account::isApproved)
                            .filter(account -> !account.isClosed())
                            .map(this::generateIban)
                            .collect(Collectors.toList());
                    return new UserSearchResultDTO(user.getId(), user.getName(), ibans);
                })
                .collect(Collectors.toList());


        return ResponseEntity.ok(results);
    }
}
