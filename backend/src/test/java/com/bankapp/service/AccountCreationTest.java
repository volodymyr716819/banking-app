package com.bankapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AccountCreationTest {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private AccountService accountService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
        testUser.setRole("CUSTOMER");
        testUser.setApproved(true);
    }
    
    @Test
    @DisplayName("Should create new account successfully")
    void shouldCreateNewAccountSuccessfully() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account savedAccount = invocation.getArgument(0);
            savedAccount.setId(1L);
            return savedAccount;
        });
        
        // When
        Account result = accountService.createAccount(1L, "CHECKING");
        
        // Then
        assertNotNull(result);
        assertEquals("CHECKING", result.getType());
        assertEquals(testUser, result.getUser());
        assertEquals(BigDecimal.ZERO, result.getBalance());
        assertFalse(result.isApproved());
        assertFalse(result.isClosed());
        verify(accountRepository).save(any(Account.class));
    }
    
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowExceptionWhenUserNotFound() {
        // Given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            accountService.createAccount(99L, "CHECKING");
        });
        
        assertEquals("User not found with id : '99'", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when user not approved")
    void shouldThrowExceptionWhenUserNotApproved() {
        // Given
        testUser.setApproved(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        // When & Then
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            accountService.createAccount(1L, "CHECKING");
        });
        
        assertEquals("User must be approved to create an account", exception.getMessage());
        verify(accountRepository, never()).save(any());
    }
}