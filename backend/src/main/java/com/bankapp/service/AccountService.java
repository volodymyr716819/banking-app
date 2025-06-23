package com.bankapp.service;

import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Creates a new account for a user
     * @param userId the ID of the user to create the account for
     * @param accountType the type of account to create (CHECKING, SAVINGS)
     * @return the created account
     * @throws ResourceNotFoundException if the user is not found
     * @throws IllegalStateException if the user is not approved
     * @throws IllegalArgumentException if the account type is invalid
     */
    @Transactional
    public Account createAccount(Long userId, String accountType) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        if (!user.isApproved()) {
            throw new IllegalStateException("User must be approved to create an account");
        }
        
        if (!"CHECKING".equals(accountType) && !"SAVINGS".equals(accountType)) {
            throw new IllegalArgumentException("Invalid account type: " + accountType);
        }
        
        Account account = new Account();
        account.setUser(user);
        account.setType(accountType);
        account.setBalance(BigDecimal.ZERO);
        account.setApproved(false);
        account.setClosed(false);
        
        return accountRepository.save(account);
    }
    
    /**
     * Approves an account
     * @param accountId the ID of the account to approve
     * @return the approved account
     * @throws ResourceNotFoundException if the account is not found
     */
    @Transactional
    public Account approveAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        account.setApproved(true);
        return accountRepository.save(account);
    }
    
    /**
     * Closes an account
     * @param accountId the ID of the account to close
     * @return the closed account
     * @throws ResourceNotFoundException if the account is not found
     * @throws IllegalStateException if the account has a non-zero balance
     */
    @Transactional
    public Account closeAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
        
        if (account.getBalance().compareTo(BigDecimal.ZERO) != 0) {
            throw new IllegalStateException("Account must have zero balance to be closed");
        }
        
        account.setClosed(true);
        return accountRepository.save(account);
    }
    
    /**
     * Gets all accounts for a user
     * @param userId the ID of the user to get accounts for
     * @return the list of accounts
     */
    public List<Account> getAccountsByUserId(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    /**
     * Gets an account by ID
     * @param accountId the ID of the account to get
     * @return the account
     * @throws ResourceNotFoundException if the account is not found
     */
    public Account getAccountById(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account", "id", accountId));
    }
}//test for merge ?