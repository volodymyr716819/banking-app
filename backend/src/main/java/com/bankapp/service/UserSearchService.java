package com.bankapp.service;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserSearchService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<UserSearchResultDTO> searchUsersByName(String name, Authentication authentication) {
        // authentication check
        if (authentication == null || userRepository.findByEmail(authentication.getName()).isEmpty()) {
            throw new IllegalArgumentException("User not authenticated");
        }
        
        // Validate input
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name search parameter is required");
        }
        
        // Find users by name
        List<User> allUsers = userRepository.findByNameContainingIgnoreCase(name.trim());
        
        // Filter users in service layer
        List<User> filteredUsers = allUsers.stream()
            .filter(user -> user.getRegistrationStatus() == RegistrationStatus.APPROVED)
            .filter(user -> "CUSTOMER".equalsIgnoreCase(user.getRole()))
            .collect(Collectors.toList());
        
        // Convert to DTOs with active accounts
        return filteredUsers.stream()
            .map(user -> {
                // Find all active accounts for this user
                List<String> ibans = accountRepository.findByUserId(user.getId()).stream()
                    .filter(Account::isApproved)
                    .filter(account -> !account.isClosed())
                    .map(Account::getIban)
                    .collect(Collectors.toList());
                    
                return new UserSearchResultDTO(user.getId(), user.getName(), ibans);
            })
            .collect(Collectors.toList());
    }
}
//test for merge ? 