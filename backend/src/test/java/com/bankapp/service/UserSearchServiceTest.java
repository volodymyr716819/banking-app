package com.bankapp.service;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserSearchService.searchUsersByName method.
 * Tests three scenarios: unapproved user, approved user with pending account, approved user with approved account.
 */
class UserSearchServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private UserSearchService userSearchService;

    private Authentication authentication;
    private User authenticatedEmployee;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create authentication for employee
        authentication = new TestingAuthenticationToken("employee@bank.com", "password", "ROLE_EMPLOYEE");
        
        // Setup authenticated employee
        authenticatedEmployee = new User();
        authenticatedEmployee.setId(1L);
        authenticatedEmployee.setEmail("employee@bank.com");
        authenticatedEmployee.setRole("EMPLOYEE");
    }

    @Test
    void searchUsersByName_UnapprovedCustomer_ReturnsEmptyList() {
        // Arrange: Create unapproved customer (Alaa Aldrobe)
        User unapprovedCustomer = new User();
        unapprovedCustomer.setId(2L);
        unapprovedCustomer.setName("Alaa Aldrobe");
        unapprovedCustomer.setRole("CUSTOMER");
        unapprovedCustomer.setRegistrationStatus(RegistrationStatus.PENDING); // Not approved yet
        
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedEmployee));
        when(userRepository.findByNameContainingIgnoreCase("Alaa")).thenReturn(Arrays.asList(unapprovedCustomer));

        // Act: Search for unapproved customer
        List<UserSearchResultDTO> result = userSearchService.searchUsersByName("Alaa", authentication);

        // Assert: Should return empty list (unapproved users are filtered out)
        assertTrue(result.isEmpty());
        verify(userRepository).findByNameContainingIgnoreCase("Alaa");
        verify(accountRepository, never()).findByUserId(anyLong()); // Should not check accounts
    }

    @Test
    void searchUsersByName_ApprovedCustomerWithPendingAccount_ReturnsCustomerWithoutIban() {
        // Arrange: Create approved customer (Trimpakkiros) with pending account
        User approvedCustomer = new User();
        approvedCustomer.setId(3L);
        approvedCustomer.setName("Trimpakkiros");
        approvedCustomer.setRole("CUSTOMER");
        approvedCustomer.setRegistrationStatus(RegistrationStatus.APPROVED);
        
        Account pendingAccount = new Account();
        pendingAccount.setId(101L);
        pendingAccount.setUser(approvedCustomer);
        pendingAccount.setIban("NL91ABNA0417164301");
        pendingAccount.setApproved(false); // Account not approved yet
        pendingAccount.setClosed(false);
        
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedEmployee));
        when(userRepository.findByNameContainingIgnoreCase("Trimpakkiros")).thenReturn(Arrays.asList(approvedCustomer));
        when(accountRepository.findByUserId(3L)).thenReturn(Arrays.asList(pendingAccount));

        // Act: Search for customer with pending account
        List<UserSearchResultDTO> result = userSearchService.searchUsersByName("Trimpakkiros", authentication);

        // Assert: Should return customer but without IBAN
        assertEquals(1, result.size());
        UserSearchResultDTO dto = result.get(0);
        assertEquals("Trimpakkiros", dto.getName());
        assertTrue(dto.getIbans().isEmpty()); // No IBANs for pending accounts
    }

    @Test
    void searchUsersByName_ApprovedCustomerWithApprovedAccount_ReturnsCustomerWithIban() {
        // Arrange: Create approved customer (Panagiotis) with approved account
        User approvedCustomer = new User();
        approvedCustomer.setId(4L);
        approvedCustomer.setName("Panagiotis");
        approvedCustomer.setRole("CUSTOMER");
        approvedCustomer.setRegistrationStatus(RegistrationStatus.APPROVED);
        
        Account approvedAccount = new Account();
        approvedAccount.setId(102L);
        approvedAccount.setUser(approvedCustomer);
        approvedAccount.setIban("NL91ABNA0417164302");
        approvedAccount.setApproved(true); // Account is approved
        approvedAccount.setClosed(false);
        approvedAccount.setBalance(new BigDecimal("1500.00"));
        
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedEmployee));
        when(userRepository.findByNameContainingIgnoreCase("Panagiotis")).thenReturn(Arrays.asList(approvedCustomer));
        when(accountRepository.findByUserId(4L)).thenReturn(Arrays.asList(approvedAccount));

        // Act: Search for customer with approved account
        List<UserSearchResultDTO> result = userSearchService.searchUsersByName("Panagiotis", authentication);

        // Assert: Should return customer with IBAN
        assertEquals(1, result.size());
        UserSearchResultDTO dto = result.get(0);
        assertEquals("Panagiotis", dto.getName());
        assertEquals(1, dto.getIbans().size());
        assertEquals("NL91ABNA0417164302", dto.getIbans().get(0));
    }
}