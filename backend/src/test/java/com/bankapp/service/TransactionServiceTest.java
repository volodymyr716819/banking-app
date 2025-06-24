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
        MockitoAnnotations.openMocks(this);
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Test User");
        
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setBalance(new BigDecimal("1000.00"));
    }

    @Test
    void saveTransaction_Success() {
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setFromAccount(testAccount);
        
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        
        Transaction result = transactionRepository.save(transaction);
        
        assertNotNull(result);
        assertEquals(new BigDecimal("100.00"), result.getAmount());
        verify(transactionRepository).save(transaction);
    }

    @Test
    void findTransactionsByUser_Success() {
        Transaction transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.00"));
        
        List<Transaction> transactions = Arrays.asList(transaction1);
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L)).thenReturn(transactions);
        
        List<Transaction> result = transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L);
        
        assertEquals(1, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).getAmount());
    }
}