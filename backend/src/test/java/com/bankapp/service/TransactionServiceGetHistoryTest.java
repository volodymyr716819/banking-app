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

class TransactionServiceGetHistoryTest {

    @Mock
    private TransactionRepository transactionRepository;
    
    @InjectMocks
    private TransactionService transactionService;

    private User customerJohn;
    private User employeeAdmin;
    private Transaction transaction1;
    private Transaction transaction2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        customerJohn = new User();
        customerJohn.setId(1L);
        customerJohn.setRole("CUSTOMER");
        
        employeeAdmin = new User();
        employeeAdmin.setId(2L);
        employeeAdmin.setRole("EMPLOYEE");
        
        transaction1 = new Transaction();
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setTimestamp(LocalDateTime.of(2024, 1, 1, 10, 0));
        
        transaction2 = new Transaction();
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setTimestamp(LocalDateTime.of(2024, 2, 1, 10, 0));
    }

    @Test
    void getTransactionHistory_CustomerViewsOwnTransactions_ShouldOnlyReturnOwnTransactions() {
        List<Transaction> customerTransactions = Arrays.asList(transaction1, transaction2);
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
            .thenReturn(customerTransactions);
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        List<TransactionHistoryDTO> result = transactionService.getTransactionHistory(filters, customerJohn);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository).findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L);
        verify(transactionRepository, never()).findAll();
    }

    @Test
    void getTransactionHistory_EmployeeViewsAllTransactions_ShouldReturnAllTransactions() {
        List<Transaction> allTransactions = Arrays.asList(transaction1, transaction2);
        when(transactionRepository.findAll()).thenReturn(allTransactions);
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        List<TransactionHistoryDTO> result = transactionService.getTransactionHistory(filters, employeeAdmin);
        
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository).findAll();
        verify(transactionRepository, never()).findByFromAccount_User_IdOrToAccount_User_Id(anyLong(), anyLong());
    }

    @Test
    void getTransactionHistory_WithDateFilter_ShouldReturnFilteredTransactions() {
        List<Transaction> allTransactions = Arrays.asList(transaction1, transaction2);
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
            .thenReturn(allTransactions);
        
        TransactionFilterRequest filters = new TransactionFilterRequest();
        filters.setStartDate("2024-01-01");
        filters.setEndDate("2024-01-31");
        
        List<TransactionHistoryDTO> result = transactionService.getTransactionHistory(filters, customerJohn);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getTimestamp().getMonth().getValue() == 1);
    }

}