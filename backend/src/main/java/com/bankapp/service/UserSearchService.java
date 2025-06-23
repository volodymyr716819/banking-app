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

import java.util.List;
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
        
        // validate input
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name search parameter is required");
        }
        
        // find users by name
        List<User> allUsers = userRepository.findByNameContainingIgnoreCase(name.trim());
        
        // filter approved customers
        List<User> filteredUsers = allUsers.stream()
            .filter(user -> user.getRegistrationStatus() == RegistrationStatus.APPROVED)
            .filter(user -> "CUSTOMER".equalsIgnoreCase(user.getRole()))
            .collect(Collectors.toList());
        
        // convert to DTOs with active accounts
        return filteredUsers.stream()
            .map(user -> {
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