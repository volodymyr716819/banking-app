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

import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransferLogicTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @Mock
    private AccountRepository accountRepository;
    
    @InjectMocks
    private TransactionService transactionService;
    
    private Account senderAccount;
    private Account receiverAccount;
    
    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setId(1L);
        sender.setName("John Smith");
        sender.setApproved(true);
        
        User receiver = new User();
        receiver.setId(2L);
        receiver.setName("Jane Doe");
        receiver.setApproved(true);
        
        senderAccount = new Account();
        senderAccount.setId(1L);
        senderAccount.setUser(sender);
        senderAccount.setType("CHECKING");
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setApproved(true);
        senderAccount.setClosed(false);
        
        receiverAccount = new Account();
        receiverAccount.setId(2L);
        receiverAccount.setUser(receiver);
        receiverAccount.setType("SAVINGS");
        receiverAccount.setBalance(new BigDecimal("500.00"));
        receiverAccount.setApproved(true);
        receiverAccount.setClosed(false);
    }
    
    @Test
    @DisplayName("Should transfer money successfully")
    void shouldTransferMoneySuccessfully() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));
        
        // When
        transactionService.transferMoney(1L, 2L, new BigDecimal("200.00"), "Test transfer");
        
        // Then
        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    @DisplayName("Should throw exception when transfer amount is zero or negative")
    void shouldThrowExceptionWhenTransferAmountIsZeroOrNegative() {
        // When & Then - Zero amount
        IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, BigDecimal.ZERO, "Test transfer");
        });
        assertEquals("Amount must be greater than zero", exceptionZero.getMessage());
        
        // When & Then - Negative amount
        IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, new BigDecimal("-50.00"), "Test transfer");
        });
        assertEquals("Amount must be greater than zero", exceptionNegative.getMessage());
        
        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when sender has insufficient balance")
    void shouldThrowExceptionWhenSenderHasInsufficientBalance() {
        // Given
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
        
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, new BigDecimal("1500.00"), "Test transfer");
        });
        
        assertEquals("Sender has insufficient balance", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}