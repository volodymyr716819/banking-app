/*package com.bankapp.service;

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

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.AtmOperation.OperationType;
import com.bankapp.model.CardDetails;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;

@ExtendWith(MockitoExtension.class)
class AtmDepositTest {

    @Mock
    private AccountRepository accountRepository;
    
    @Mock
    private CardDetailsRepository cardDetailsRepository;
    
    @Mock
    private AtmOperationRepository atmOperationRepository;
    
    @Mock
    private PinHashUtil pinHashUtil;
    
    @InjectMocks
    private AtmService atmService;
    
    private Account account;
    private CardDetails cardDetails;
    
    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(1L);
        user.setName("John Smith");
        user.setApproved(true);
        
        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setType("CHECKING");
        account.setBalance(new BigDecimal("1000.00"));
        account.setApproved(true);
        account.setClosed(false);
        
        cardDetails = new CardDetails();
        cardDetails.setId(1L);
        cardDetails.setAccount(account);
        cardDetails.setHashedPin("hashedPin1234");
        cardDetails.setPinCreated(true);
    }
    
    @Test
    @DisplayName("Should process ATM deposit successfully")
    void shouldProcessAtmDepositSuccessfully() {
        // Given
        String pin = "1234";
        BigDecimal depositAmount = new BigDecimal("500.00");
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cardDetailsRepository.findByAccountId(1L)).thenReturn(Optional.of(cardDetails));
        when(pinHashUtil.verifyPin(pin, cardDetails.getHashedPin())).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenReturn(account);
        when(atmOperationRepository.save(any(AtmOperation.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
       AtmOperation result = atmService.performAtmOperation(1L, depositAmount, pin, OperationType.DEPOSIT);
        
        // Then
        assertNotNull(result);
        assertEquals(AtmOperation.OperationType.DEPOSIT, result.getOperationType());
        assertEquals(depositAmount, result.getAmount());
        assertEquals(new BigDecimal("1500.00"), account.getBalance());
        verify(accountRepository).save(account);
        verify(atmOperationRepository).save(any(AtmOperation.class));
    }
    
    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        // Given
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());
        
        // When & Then
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            atmService.performAtmOperation(99L, new BigDecimal("500.00"), "1234", OperationType.DEPOSIT);
        });
        
        assertEquals("Account not found with id : '99'", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(atmOperationRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when PIN verification fails")
    void shouldThrowExceptionWhenPinVerificationFails() {
        // Given
        String incorrectPin = "9999";
        
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cardDetailsRepository.findByAccountId(1L)).thenReturn(Optional.of(cardDetails));
        when(pinHashUtil.verifyPin(incorrectPin, cardDetails.getHashedPin())).thenReturn(false);
        
        // When & Then
        InvalidPinException exception = assertThrows(InvalidPinException.class, () -> {
            atmService.performAtmOperation(1L, new BigDecimal("500.00"), incorrectPin, OperationType.DEPOSIT);
        });
        
        assertEquals("Invalid PIN provided", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(atmOperationRepository, never()).save(any());
    }
}*/