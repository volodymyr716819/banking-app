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
import java.util.stream.Collectors;

@Service
public class UserSearchService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    public List<UserSearchResultDTO> searchUsers(String term, String name, String email, String iban, Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("User not authenticated");
        }

        Optional<User> userOpt = userRepository.findByEmail(authentication.getName());
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        if (!userOpt.get().isApproved()) {
            throw new IllegalArgumentException("User not approved");
        }

        List<User> matchingUsers = new ArrayList<>();

        if (term != null && !term.trim().isEmpty()) {
            matchingUsers = userRepository.findApprovedCustomersBySearchTerm(term.trim());

            if (matchingUsers.isEmpty() && IbanGenerator.validateIban(term.trim())) {
                try {
                    Long accountId = IbanGenerator.extractAccountId(term.trim());
                    Optional<Account> account = accountRepository.findById(accountId);
                    if (account.isPresent() && account.get().isApproved() && !account.get().isClosed()) {
                        User accountOwner = account.get().getUser();
                        if (accountOwner.isApproved() && "CUSTOMER".equalsIgnoreCase(accountOwner.getRole())) {
                            matchingUsers = List.of(accountOwner);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid IBAN format, ignore
                }
            }
        } else {
            if ((name == null || name.trim().isEmpty()) &&
                (email == null || email.trim().isEmpty()) &&
                (iban == null || iban.trim().isEmpty())) {
                throw new IllegalArgumentException("At least one search parameter is required");
            }

            if (iban != null && !iban.trim().isEmpty() && IbanGenerator.validateIban(iban.trim())) {
                try {
                    Long accountId = IbanGenerator.extractAccountId(iban.trim());
                    Optional<Account> accountOpt = accountRepository.findById(accountId);
                    if (accountOpt.isPresent() && accountOpt.get().isApproved() && !accountOpt.get().isClosed()) {
                        User accountOwner = accountOpt.get().getUser();
                        if (accountOwner.isApproved() && "CUSTOMER".equalsIgnoreCase(accountOwner.getRole())) {
                            matchingUsers.add(accountOwner);
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Invalid IBAN format, ignore
                }
            } else {
                matchingUsers = userRepository.searchApprovedByNameEmailAndRole(
                        (name != null && !name.trim().isEmpty()) ? name : null,
                        (email != null && !email.trim().isEmpty()) ? email : null,
                        "customer");
            }
        }

        return matchingUsers.stream()
                .map(user -> {
                    List<Account> accounts = accountRepository.findByUserId(user.getId());
                    List<String> ibans = accounts.stream()
                            .filter(Account::isApproved)
                            .filter(account -> !account.isClosed())
                            .map(account -> IbanGenerator.generateIban(account.getId()))  
                            .collect(Collectors.toList());
                    return new UserSearchResultDTO(user.getId(), user.getName(), ibans);
                })
                .collect(Collectors.toList());
    }
} 
