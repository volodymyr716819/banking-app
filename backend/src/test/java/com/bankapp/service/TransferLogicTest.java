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
    private TransactionRepository transactionRepository; // Mock transaction DB operations
    
    @Mock
    private AccountRepository accountRepository;       // Mock account DB operations
    
    @InjectMocks
    private TransactionService transactionService;    // The service being tested
    
    
    private Account senderAccount;
    private Account receiverAccount;
    
    @BeforeEach
    void setUp() {
        // Create dummy sender user and account
        User sender = new User();
        sender.setId(1L);
        sender.setName("John Smith");
        sender.setApproved(true);
        
        senderAccount = new Account();
        senderAccount.setId(1L);
        senderAccount.setUser(sender);
        senderAccount.setType("CHECKING");
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setApproved(true);
        senderAccount.setClosed(false);
        
        // Create dummy receiver user and account
        User receiver = new User();
        receiver.setId(2L);
        receiver.setName("Jane Doe");
        receiver.setApproved(true);
        
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
        // Mock database calls to return dummy accounts
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));            // Simulate saving account
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));   // Simulate saving transaction
        
        // Perform transfer of 200 from sender to receiver
        transactionService.transferMoney(1L, 2L, new BigDecimal("200.00"), "Test transfer");
        
        // Verify balances updated correctly
        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());
        
        // Verify that save methods were called
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }
    
    @Test
    @DisplayName("Should throw exception when transfer amount is zero or negative")
    void shouldThrowExceptionWhenTransferAmountIsZeroOrNegative() {
        // Test zero amount - expect exception
        IllegalArgumentException exceptionZero = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, BigDecimal.ZERO, "Test transfer");
        });
        assertEquals("Amount must be greater than zero", exceptionZero.getMessage());
        
        // Test negative amount - expect exception
        IllegalArgumentException exceptionNegative = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, new BigDecimal("-50.00"), "Test transfer");
        });
        assertEquals("Amount must be greater than zero", exceptionNegative.getMessage());
        
        // Verify no DB calls were made because validation failed early
        verify(accountRepository, never()).findById(any());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
    
    @Test
    @DisplayName("Should throw exception when sender has insufficient balance")
    void shouldThrowExceptionWhenSenderHasInsufficientBalance() {
        // Mock accounts returned from DB
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
        
        // Try to transfer more than sender's balance - expect exception
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney(1L, 2L, new BigDecimal("1500.00"), "Test transfer");
        });
        
        // Verify no saves were called because transfer aborted
        assertEquals("Sender has insufficient balance", exception.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}