package com.bankapp.service;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserSearchService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    
    // Constructor injection for dependencies
    public UserSearchService(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    // Main search method - finds users by name with security checks
    public List<UserSearchResultDTO> searchUsersByName(String name, Authentication authentication) {
    validateAuth(authentication);    // Step 1: Check if user is logged in
    validateInput(name);            // Step 2: Check if search term is valid
    
    List<User> filteredUsers = findApprovedCustomers(name);  // Step 3: Find users
    return filteredUsers.stream()
        .map(this::toDTO)           // Step 4: Convert to DTOs
        .collect(Collectors.toList());
}
    
    // Checks if authentication object exists and  user exists in database by email
    private void validateAuth(Authentication authentication) {
        if (authentication == null || userRepository.findByEmail(authentication.getName()).isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }
    }
    
    // Checks if name is not null and not just whitespace
    private void validateInput(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name search parameter is required");
        }
    }
    
    // Searches database for users whose name contains the search term (case-insensitive)
    private List<User> findApprovedCustomers(String name) {
        List<User> allUsers = userRepository.findByNameContainingIgnoreCase(name.trim());
        return allUsers.stream()
        // 1. Users with APPROVED status (not pending or rejected)
            .filter(user -> user.getRegistrationStatus() == RegistrationStatus.APPROVED)
              // 2. Users with CUSTOMER role (not employees)
            .filter(user -> "CUSTOMER".equalsIgnoreCase(user.getRole()))
            .collect(Collectors.toList());
    }
    
    // Converts User entity to DTO with active account IBANs
    private UserSearchResultDTO toDTO(User user) {
        // Get only approved and open accounts for the user
        List<String> ibans = accountRepository.findByUserId(user.getId()).stream()
            .filter(Account::isApproved)
            .filter(account -> !account.isClosed())
            .map(Account::getIban)
            .collect(Collectors.toList());
            
        return new UserSearchResultDTO(user.getId(), user.getName(), ibans);
    }
}