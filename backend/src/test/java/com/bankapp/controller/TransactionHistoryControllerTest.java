package com.bankapp.controller;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.TransactionService;
import com.bankapp.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionHistoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TransactionService transactionService;
    
    @MockBean
    private UserRepository userRepository;
    
    @MockBean
    private TransactionRepository transactionRepository;

    @Test
    @WithMockUser(username = "alaa@example.com", roles = "CUSTOMER")
    void customerViewsOwnTransactions() throws Exception {
        // Arrange: Mock transaction history response
        TransactionHistoryDTO tx1 = new TransactionHistoryDTO();
        tx1.setTransactionId(1L);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setTransactionType("TRANSFER");
        tx1.setTimestamp(LocalDateTime.now());
        
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("alaa@example.com");
        
        when(userRepository.findByEmail("alaa@example.com")).thenReturn(Optional.of(customer));
        when(transactionService.getTransactionHistory(any(), eq(customer)))
            .thenReturn(Arrays.asList(tx1));

        // Act & Assert: GET /api/transactions/history
        mockMvc.perform(get("/api/transactions/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"));
    }

    @Test
    @WithMockUser(username = "employee@bank.com", roles = "EMPLOYEE")
    void employeeViewsSpecificUserTransactions() throws Exception {
        // Arrange: Employee views customer's transactions
        TransactionHistoryDTO tx1 = new TransactionHistoryDTO();
        tx1.setTransactionId(2L);
        tx1.setAmount(new BigDecimal("200.00"));
        tx1.setTransactionType("DEPOSIT");
        
        User employee = new User();
        employee.setId(3L);
        employee.setEmail("employee@bank.com");
        employee.setRole("EMPLOYEE");
        
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(employee));
        when(transactionService.getTransactionHistory(any(), eq(employee)))
            .thenReturn(Arrays.asList(tx1));

        // Act & Assert: GET /api/transactions/history?userId=2
        mockMvc.perform(get("/api/transactions/history")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    @Test
    @WithMockUser(username = "alaa@example.com", roles = "CUSTOMER")
    void customerCannotViewOthersTransactions() throws Exception {
        // Arrange: Customer tries to view another user's transactions
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("alaa@example.com");
        customer.setRole("CUSTOMER");
        
        when(userRepository.findByEmail("alaa@example.com")).thenReturn(Optional.of(customer));
        when(transactionService.getTransactionHistory(any(), eq(customer)))
            .thenReturn(Arrays.asList()); // Service ignores userId param for customers

        // Act & Assert: Customer tries to access other user's data
        mockMvc.perform(get("/api/transactions/history")
                .param("userId", "2")) // Trying to view user 2's transactions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        
        // Verify service was called with the customer's context, not user 2
        verify(transactionService).getTransactionHistory(any(), eq(customer));
    }
}