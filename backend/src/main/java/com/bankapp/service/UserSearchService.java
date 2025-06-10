package com.bankapp.service;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.util.IbanGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserSearchService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;
    
    // Sets the user repository for this service. Used for dependency injection in testing.
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    // Sets the account repository for this service. Used for dependency injection in testing.
    public void setAccountRepository(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // Searches for customers based on the provided criteria and returns matching customer information.
    public List<UserSearchResultDTO> searchUsers(String term, String name, String email, String iban, Authentication authentication) {
        // Check if user is authenticated
        if (authentication == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        // Find the user who is making the search
        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        // Check if user is approved
        if (!userOpt.get().isApproved()) {
            throw new IllegalArgumentException("User not approved");
        }

        // Search for matching customers
        List<User> matchingUsers = findMatchingCustomers(term, name, email, iban);
        
        // Convert users to DTOs with IBANs
        List<UserSearchResultDTO> results = new ArrayList<>();
        for (User customer : matchingUsers) {
            UserSearchResultDTO dto = createSearchResult(customer);
            results.add(dto);
        }
        
        return results;
    }

    // Finds customers matching the specified search criteria.
    private List<User> findMatchingCustomers(String term, String name, String email, String iban) {
        // If we have a general search term
        if (term != null && !term.trim().isEmpty()) {
            return findCustomersByTerm(term.trim());
        }
        
        // Check if we have any search criteria
        if ((name == null || name.trim().isEmpty()) &&
            (email == null || email.trim().isEmpty()) &&
            (iban == null || iban.trim().isEmpty())) {
            throw new IllegalArgumentException("At least one search parameter is required");
        }

        // If we have an IBAN
        if (iban != null && !iban.trim().isEmpty()) {
            return findCustomersByIban(iban.trim());
        }
        
        // Search by name and/or email
        return userRepository.searchApprovedByNameEmailAndRole(
            (name != null && !name.trim().isEmpty()) ? name.trim() : null,
            (email != null && !email.trim().isEmpty()) ? email.trim() : null,
            "customer");
    }
    
    // Searches for customers using a general search term.
    private List<User> findCustomersByTerm(String term) {
        // Try to find users by name or email
        List<User> users = userRepository.findApprovedCustomersBySearchTerm(term);
        
        // If no users found and term looks like an IBAN, try searching by IBAN
        if (users.isEmpty() && IbanGenerator.validateIban(term)) {
            users = findCustomersByIban(term);
        }
        
        return users;
    }
    
    // Finds customers by their account IBAN.
    private List<User> findCustomersByIban(String iban) {
        List<User> result = new ArrayList<>();
        
        // Check if IBAN is valid
        if (!IbanGenerator.validateIban(iban)) {
            throw new IllegalArgumentException("Invalid IBAN format");
        }
        
        try {
            // Extract account ID from IBAN
            Long accountId = IbanGenerator.extractAccountId(iban);
            
            // Find the account
            Optional<Account> accountOpt = accountRepository.findById(accountId);
            
            // Check if account exists and is active
            if (accountOpt.isPresent() && 
                accountOpt.get().isApproved() && 
                !accountOpt.get().isClosed()) {
                
                // Get the account owner
                User accountOwner = accountOpt.get().getUser();
                
                // Check if owner is an approved customer
                if (accountOwner.isApproved() && 
                    "CUSTOMER".equalsIgnoreCase(accountOwner.getRole())) {
                    result.add(accountOwner);
                }
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid IBAN format: " + e.getMessage());
        }
        
        return result;
    }
    
    // Creates a search result DTO containing customer information and account IBANs.
    private UserSearchResultDTO createSearchResult(User customer) {
        // Get all accounts for this customer
        List<Account> accounts = accountRepository.findByUserId(customer.getId());
        
        // Get IBANs from active accounts
        List<String> ibans = new ArrayList<>();
        for (Account account : accounts) {
            // Only include approved and not closed accounts
            if (account.isApproved() && !account.isClosed()) {
                String iban = IbanGenerator.generateIban(account.getId());
                ibans.add(iban);
            }
        }
        
        // Create and return result DTO
        return new UserSearchResultDTO(customer.getId(), customer.getName(), ibans);
    }
}