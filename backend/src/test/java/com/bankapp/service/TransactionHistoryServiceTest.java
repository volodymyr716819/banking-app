package com.bankapp.service;

import com.bankapp.dto.TransactionFilterRequest;
import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.model.*;
import com.bankapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TransactionService.getTransactionHistory method.
 * Tests three scenarios: customer views own, employee views any user's, customer can't view others.
 */
class TransactionHistoryServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private com.bankapp.repository.AtmOperationRepository atmOperationRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User customerAlaa;
    private User customerPanagiotis;
    private User employeeUser;
    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup test users
        customerAlaa = new User();
        customerAlaa.setId(1L);
        customerAlaa.setName("Alaa Aldrobe");
        customerAlaa.setRole("CUSTOMER");
        
        customerPanagiotis = new User();
        customerPanagiotis.setId(2L);
        customerPanagiotis.setName("Panagiotis");
        customerPanagiotis.setRole("CUSTOMER");
        
        employeeUser = new User();
        employeeUser.setId(3L);
        employeeUser.setName("Employee");
        employeeUser.setRole("EMPLOYEE");
        
        // Setup test transactions
        transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setTimestamp(LocalDateTime.now());
        
        transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setTimestamp(LocalDateTime.now());
    }

    @Test
    void customerCanViewOwnTransactions() {
        // Arrange: Alaa has transactions
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
            .thenReturn(Arrays.asList(transaction1, transaction2));
        when(atmOperationRepository.findByAccount_User_Id(1L))
            .thenReturn(Arrays.asList());
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        
        // Act: Alaa requests her own transactions
        List<TransactionHistoryDTO> result = transactionService.getTransactionHistory(filters, customerAlaa);
        
        // Assert: Should return Alaa's transactions
        assertEquals(2, result.size());
        verify(transactionRepository).findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L);
    }

    @Test
    void employeeCanViewSpecificUserTransactions() {
        // Arrange: Employee wants to view Panagiotis' transactions
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(2L, 2L))
            .thenReturn(Arrays.asList(transaction1));
        when(atmOperationRepository.findByAccount_User_Id(2L))
            .thenReturn(Arrays.asList());
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        filters.setUserId(2L); // Employee specifies Panagiotis' ID
        
        // Act: Employee requests Panagiotis' transactions
        List<TransactionHistoryDTO> result = transactionService.getTransactionHistory(filters, employeeUser);
        
        // Assert: Should return Panagiotis' transactions
        assertEquals(1, result.size());
        verify(transactionRepository).findByFromAccount_User_IdOrToAccount_User_Id(2L, 2L);
    }

    @Test
    void customerCannotViewOtherUserTransactions() {
        // Arrange: Alaa tries to view Panagiotis' transactions
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
            .thenReturn(Arrays.asList(transaction1));
        when(atmOperationRepository.findByAccount_User_Id(1L))
            .thenReturn(Arrays.asList());
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        filters.setUserId(2L); // Alaa tries to specify Panagiotis' ID
        
        // Act: Alaa requests transactions (should ignore userId filter)
        transactionService.getTransactionHistory(filters, customerAlaa);
        
        // Assert: Should return only Alaa's transactions, not Panagiotis'
        verify(transactionRepository).findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L);
        verify(transactionRepository, never()).findByFromAccount_User_IdOrToAccount_User_Id(2L, 2L);
    }
}