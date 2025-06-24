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
 * 
 * This test class demonstrates basic unit testing practices with JUnit 5 and Mockito:
 * - Mocking external dependencies (repositories)
 * - Setting up test data in @BeforeEach
 * - Testing different scenarios (happy path, error cases, edge cases)
 * - Using simple assertions to verify behavior
 */
class UserSearchServiceTest {

    // Mock the UserRepository dependency to control its behavior in tests
    @Mock
    private UserRepository userRepository;

    // Mock the AccountRepository dependency to control its behavior in tests
    @Mock
    private AccountRepository accountRepository;

    // Create the service under test and inject the mocked dependencies
    @InjectMocks
    private UserSearchService userSearchService;

    // Mock authentication object for testing authorization
    @Mock
    private Authentication authentication;

    // Test data - users that we'll use across multiple tests
    private User authenticatedUser;
    private User approvedCustomer1;
    private User approvedCustomer2;
    private User pendingCustomer;
    private User employeeUser;
    
    // Test data - accounts for the users
    private Account approvedAccount;
    private Account closedAccount;
    private Account unapprovedAccount;

    /**
     * Set up test data before each test method runs.
     * This method creates all the test objects we'll need for our tests.
     */
    @BeforeEach
    void setUp() {
        // Initialize Mockito annotations (@Mock, @InjectMocks)
        MockitoAnnotations.openMocks(this);
        
        // Create an authenticated user (the one performing the search)
        authenticatedUser = new User();
        authenticatedUser.setId(1L);
        authenticatedUser.setEmail("employee@bank.com");
        authenticatedUser.setName("Bank Employee");
        authenticatedUser.setRole("EMPLOYEE");
        
        // Create test customers with different scenarios
        
        // Approved customer 1 - Alaa Aldrobe (happy path case)
        approvedCustomer1 = new User();
        approvedCustomer1.setId(2L);
        approvedCustomer1.setName("Alaa Aldrobe");
        approvedCustomer1.setEmail("alaa@example.com");
        approvedCustomer1.setRole("CUSTOMER");
        approvedCustomer1.setRegistrationStatus(RegistrationStatus.APPROVED);
        
        // Approved customer 2 - Trimpakkiros, Panagiotis (testing name search)
        approvedCustomer2 = new User();
        approvedCustomer2.setId(3L);
        approvedCustomer2.setName("Trimpakkiros, Panagiotis");
        approvedCustomer2.setEmail("panagiotis@example.com");
        approvedCustomer2.setRole("CUSTOMER");
        approvedCustomer2.setRegistrationStatus(RegistrationStatus.APPROVED);
        
        // Pending customer - should be filtered out
        pendingCustomer = new User();
        pendingCustomer.setId(4L);
        pendingCustomer.setName("DanBreczinski");
        pendingCustomer.setEmail("dan@example.com");
        pendingCustomer.setRole("CUSTOMER");
        pendingCustomer.setRegistrationStatus(RegistrationStatus.PENDING);
        
        // Employee user - should be filtered out (not a customer)
        employeeUser = new User();
        employeeUser.setId(5L);
        employeeUser.setName("John Employee");
        employeeUser.setEmail("john@bank.com");
        employeeUser.setRole("EMPLOYEE");
        employeeUser.setRegistrationStatus(RegistrationStatus.APPROVED);
        
        // Create test accounts
        
        // Approved and open account
        approvedAccount = new Account();
        approvedAccount.setId(101L);
        approvedAccount.setUser(approvedCustomer1);
        approvedAccount.setIban("NL91ABNA0417164300");
        approvedAccount.setBalance(new BigDecimal("1000.00"));
        approvedAccount.setApproved(true);
        approvedAccount.setClosed(false);
        
        // Closed account - IBAN should not be included in results
        closedAccount = new Account();
        closedAccount.setId(102L);
        closedAccount.setUser(approvedCustomer1);
        closedAccount.setIban("NL91ABNA0417164301");
        closedAccount.setBalance(new BigDecimal("0.00"));
        closedAccount.setApproved(true);
        closedAccount.setClosed(true);
        
        // Unapproved account - IBAN should not be included in results
        unapprovedAccount = new Account();
        unapprovedAccount.setId(103L);
        unapprovedAccount.setUser(approvedCustomer2);
        unapprovedAccount.setIban("NL91ABNA0417164302");
        unapprovedAccount.setBalance(new BigDecimal("500.00"));
        unapprovedAccount.setApproved(false);
        unapprovedAccount.setClosed(false);
    }

    /**
     * Test the happy path: successful search returning approved customers with their IBANs.
     * This test verifies that when everything works correctly:
     * - The search finds approved customers
     * - Only approved and open accounts are included
     * - The correct user information is returned
     */
    @Test
    void searchUsersByName_HappyPath_ReturnsApprovedCustomersWithIbans() {
        // Arrange: Set up what the mocks should return
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByNameContainingIgnoreCase("Alaa")).thenReturn(Arrays.asList(approvedCustomer1));
        when(accountRepository.findByUserId(2L)).thenReturn(Arrays.asList(approvedAccount, closedAccount));
        
        // Act: Call the method we're testing
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("Alaa", authentication);
        
        // Assert: Verify the results are what we expect
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.size(), "Should find exactly 1 user");
        
        UserSearchResultDTO result = results.get(0);
        assertEquals(2L, result.getId(), "Should return the correct user ID");
        assertEquals("Alaa Aldrobe", result.getName(), "Should return the correct user name");
        assertEquals(1, result.getIbans().size(), "Should return only approved and open accounts");
        assertEquals("NL91ABNA0417164300", result.getIbans().get(0), "Should return the correct IBAN");
        
        // Verify that the correct repository methods were called
        verify(userRepository).findByEmail("employee@bank.com");
        verify(userRepository).findByNameContainingIgnoreCase("Alaa");
        verify(accountRepository).findByUserId(2L);
    }

    /**
     * Test authentication validation: search with null authentication should throw exception.
     * This test ensures that users must be authenticated to perform searches.
     */
    @Test
    void searchUsersByName_NullAuthentication_ThrowsException() {
        // Act & Assert: Verify that calling with null authentication throws an exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userSearchService.searchUsersByName("Alaa", null),
            "Should throw exception when authentication is null"
        );
        
        assertEquals("User not authenticated", exception.getMessage(), "Should have correct error message");
        
        // Verify that no repository methods were called since authentication failed
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByNameContainingIgnoreCase(anyString());
    }

    /**
     * Test input validation: search with null name parameter should throw exception.
     * This test ensures that a search term is required.
     */
    @Test
    void searchUsersByName_NullName_ThrowsException() {
        // Arrange: Set up authentication to pass, but use null name
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        
        // Act & Assert: Verify that calling with null name throws an exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userSearchService.searchUsersByName(null, authentication),
            "Should throw exception when name is null"
        );
        
        assertEquals("Name search parameter is required", exception.getMessage(), "Should have correct error message");
        
        // Verify authentication was checked but search was not performed
        verify(userRepository).findByEmail("employee@bank.com");
        verify(userRepository, never()).findByNameContainingIgnoreCase(anyString());
    }

    /**
     * Test input validation: search with empty name parameter should throw exception.
     * This test ensures that an empty string is not accepted as a search term.
     */
    @Test
    void searchUsersByName_EmptyName_ThrowsException() {
        // Arrange: Set up authentication to pass, but use empty name
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        
        // Act & Assert: Verify that calling with empty name throws an exception
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> userSearchService.searchUsersByName("   ", authentication), // spaces only
            "Should throw exception when name is empty/whitespace"
        );
        
        assertEquals("Name search parameter is required", exception.getMessage(), "Should have correct error message");
    }

    /**
     * Test user filtering: search that filters out non-approved users.
     * This test verifies that only users with APPROVED status are returned.
     */
    @Test
    void searchUsersByName_FiltersOutPendingUsers_ReturnsOnlyApproved() {
        // Arrange: Repository returns both approved and pending users, but service should filter out pending
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByNameContainingIgnoreCase("Dan")).thenReturn(Arrays.asList(pendingCustomer));
        
        // Act: Search for users that include pending ones
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("Dan", authentication);
        
        // Assert: Should return empty list since pending users are filtered out
        assertNotNull(results, "Results should not be null");
        assertEquals(0, results.size(), "Should not return pending users");
        
        // Verify the search was attempted
        verify(userRepository).findByNameContainingIgnoreCase("Dan");
    }

    /**
     * Test role filtering: search that filters out non-customer users.
     * This test verifies that only users with CUSTOMER role are returned.
     */
    @Test
    void searchUsersByName_FiltersOutEmployees_ReturnsOnlyCustomers() {
        // Arrange: Repository returns employee user, but service should filter out non-customers
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByNameContainingIgnoreCase("John")).thenReturn(Arrays.asList(employeeUser));
        
        // Act: Search for users that include employee
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("John", authentication);
        
        // Assert: Should return empty list since employees are filtered out
        assertNotNull(results, "Results should not be null");
        assertEquals(0, results.size(), "Should not return employee users");
        
        // Verify the search was attempted
        verify(userRepository).findByNameContainingIgnoreCase("John");
    }

    /**
     * Test account handling: search returning users with no accounts.
     * This test verifies that users without accounts are still returned, just with empty IBAN list.
     */
    @Test
    void searchUsersByName_UserWithNoAccounts_ReturnsUserWithEmptyIbanList() {
        // Arrange: User exists but has no accounts
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByNameContainingIgnoreCase("Panagiotis")).thenReturn(Arrays.asList(approvedCustomer2));
        when(accountRepository.findByUserId(3L)).thenReturn(Collections.emptyList()); // No accounts
        
        // Act: Search for user with no accounts
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("Panagiotis", authentication);
        
        // Assert: Should return the user but with empty IBAN list
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.size(), "Should find the user even without accounts");
        
        UserSearchResultDTO result = results.get(0);
        assertEquals("Trimpakkiros, Panagiotis", result.getName(), "Should return correct user name");
        assertTrue(result.getIbans().isEmpty(), "Should have empty IBAN list when user has no accounts");
    }

    /**
     * Test account filtering: search returning users with closed/non-approved accounts.
     * This test verifies that only approved and open accounts have their IBANs included.
     */
    @Test
    void searchUsersByName_UserWithClosedAndUnapprovedAccounts_ExcludesThoseIbans() {
        // Arrange: User has mix of approved/unapproved and open/closed accounts
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        when(userRepository.findByNameContainingIgnoreCase("Panagiotis")).thenReturn(Arrays.asList(approvedCustomer2));
        // Return accounts that should be filtered out
        when(accountRepository.findByUserId(3L)).thenReturn(Arrays.asList(closedAccount, unapprovedAccount));
        
        // Act: Search for user with problematic accounts
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("Panagiotis", authentication);
        
        // Assert: Should return the user but with no IBANs (since all accounts are closed or unapproved)
        assertNotNull(results, "Results should not be null");
        assertEquals(1, results.size(), "Should find the user");
        
        UserSearchResultDTO result = results.get(0);
        assertEquals("Trimpakkiros, Panagiotis", result.getName(), "Should return correct user name");
        assertTrue(result.getIbans().isEmpty(), "Should exclude IBANs from closed or unapproved accounts");
    }

    /**
     * Test comprehensive scenario: multiple users with different account states.
     * This test verifies the complete filtering logic works correctly with realistic data.
     */
    @Test
    void searchUsersByName_MultipleUsersWithMixedData_ReturnsCorrectResults() {
        // Arrange: Create a scenario with multiple users and various account states
        
        // Create another approved account for customer2
        Account approvedAccountForCustomer2 = new Account();
        approvedAccountForCustomer2.setId(104L);
        approvedAccountForCustomer2.setUser(approvedCustomer2);
        approvedAccountForCustomer2.setIban("NL91ABNA0417164304");
        approvedAccountForCustomer2.setApproved(true);
        approvedAccountForCustomer2.setClosed(false);
        
        when(authentication.getName()).thenReturn("employee@bank.com");
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(authenticatedUser));
        // Search returns multiple users: approved customers, pending customer, and employee
        when(userRepository.findByNameContainingIgnoreCase("a")).thenReturn(
            Arrays.asList(approvedCustomer1, approvedCustomer2, pendingCustomer, employeeUser)
        );
        
        // Set up accounts for each user
        when(accountRepository.findByUserId(2L)).thenReturn(Arrays.asList(approvedAccount)); // Customer1: 1 good account
        when(accountRepository.findByUserId(3L)).thenReturn(Arrays.asList(approvedAccountForCustomer2, unapprovedAccount)); // Customer2: 1 good, 1 bad
        when(accountRepository.findByUserId(4L)).thenReturn(Collections.emptyList()); // Pending customer: no accounts
        when(accountRepository.findByUserId(5L)).thenReturn(Collections.emptyList()); // Employee: no accounts
        
        // Act: Search with a broad term that matches multiple users
        List<UserSearchResultDTO> results = userSearchService.searchUsersByName("a", authentication);
        
        // Assert: Should only return the 2 approved customers, with correct IBANs
        assertNotNull(results, "Results should not be null");
        assertEquals(2, results.size(), "Should return only approved customers");
        
        // Find results by name to make assertions more reliable
        UserSearchResultDTO alaaResult = results.stream()
            .filter(r -> r.getName().equals("Alaa Aldrobe"))
            .findFirst()
            .orElse(null);
        assertNotNull(alaaResult, "Should find Alaa Aldrobe in results");
        assertEquals(1, alaaResult.getIbans().size(), "Alaa should have 1 IBAN");
        
        UserSearchResultDTO panagiotisResult = results.stream()
            .filter(r -> r.getName().equals("Trimpakkiros, Panagiotis"))
            .findFirst()
            .orElse(null);
        assertNotNull(panagiotisResult, "Should find Panagiotis in results");
        assertEquals(1, panagiotisResult.getIbans().size(), "Panagiotis should have 1 IBAN (unapproved account filtered out)");
    }
}