package com.bankapp.service;

import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.model.Account;
import com.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account testAccount;
    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Initialize mocks before each test
        
        testUser = new User();
        testUser.setId(1L);               // Setup user ID
        testUser.setName("Test User");   // Setup user name
        
        testAccount = new Account();
        testAccount.setId(1L);                                // Setup account ID
        testAccount.setUser(testUser);                       // Link account to user
        testAccount.setBalance(new BigDecimal("1000.00"));  // Set initial balance
    }

    @Test
    void saveTransaction_Success() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));   // Set transaction amount
        transaction.setFromAccount(testAccount);          // Set sender account
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);    // Mock save behavior
        
        Transaction result = transactionRepository.save(transaction);    // Call save
        
        assertNotNull(result);     // Verify result is not null
        assertEquals(new BigDecimal("100.00"), result.getAmount());     // Verify amount matches
        verify(transactionRepository).save(transaction);               // Confirm save called once
    }

    @Test
    void findTransactionsByUser_Success() {
        Transaction transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.00"));    // Setup transaction amount
        
        List<Transaction> transactions = Arrays.asList(transaction1);
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L)).thenReturn(transactions); // Mock find
        
        List<Transaction> result = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L); // Call find
        
        assertEquals(1, result.size());     // Check returned list size
        assertEquals(new BigDecimal("100.00"), result.get(0).getAmount()); // Verify amount in first transaction
    }
}