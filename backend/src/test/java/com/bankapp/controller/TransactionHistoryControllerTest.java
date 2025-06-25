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

    //Test 1: Customer Views Own Transactions
    @Test
    @WithMockUser(username = "alaa@example.com", roles = "CUSTOMER")
    void customerViewsOwnTransactions() throws Exception {
        // ARRANGE: Create test transaction
        TransactionHistoryDTO tx1 = new TransactionHistoryDTO();
        tx1.setTransactionId(1L);
        tx1.setAmount(new BigDecimal("100.00"));
        tx1.setTransactionType("TRANSFER");
        tx1.setTimestamp(LocalDateTime.now());
         // Create customer user
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("alaa@example.com");
        // Mock repository and service
        when(userRepository.findByEmail("alaa@example.com")).thenReturn(Optional.of(customer));
        when(transactionService.getTransactionHistory(any(), eq(customer)))
            .thenReturn(Arrays.asList(tx1));

       // ACT & ASSERT: Call endpoint
        mockMvc.perform(get("/api/transactions/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[0].transactionType").value("TRANSFER"));
    }

    //Test 2: Employee Views Specific User's Transactions
    @Test
    @WithMockUser(username = "employee@bank.com", roles = "EMPLOYEE")
    void employeeViewsSpecificUserTransactions() throws Exception {
      // ARRANGE: Create transaction for viewing
        TransactionHistoryDTO tx1 = new TransactionHistoryDTO();
        tx1.setTransactionId(2L);
        tx1.setAmount(new BigDecimal("200.00"));
        tx1.setTransactionType("DEPOSIT");
        // Create employee user
        User employee = new User();
        employee.setId(3L);
        employee.setEmail("employee@bank.com");
        employee.setRole("EMPLOYEE");
        
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(employee));
        when(transactionService.getTransactionHistory(any(), eq(employee)))
            .thenReturn(Arrays.asList(tx1));

        // ACT & ASSERT: Employee requests specific user's transactions
        mockMvc.perform(get("/api/transactions/history")
                .param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amount").value(200.00));
    }

    //Test 3: Security Test - Customer Cannot View Others' Transactions
    @Test
    @WithMockUser(username = "alaa@example.com", roles = "CUSTOMER")
    void customerCannotViewOthersTransactions() throws Exception {
       // ARRANGE: Customer setup
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("alaa@example.com");
        customer.setRole("CUSTOMER");
        
        when(userRepository.findByEmail("alaa@example.com")).thenReturn(Optional.of(customer));
        when(transactionService.getTransactionHistory(any(), eq(customer)))
            .thenReturn(Arrays.asList()); // Service ignores userId param for customers

        // ACT: Customer tries to hack by adding userId parameter
        mockMvc.perform(get("/api/transactions/history")
                .param("userId", "2")) // Trying to view user 2's transactions
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        
        // ASSERT: Verify service was called with customer, NOT user 2
        verify(transactionService).getTransactionHistory(any(), eq(customer));
    }
     //Security protection - customers can't view others' data 
    //userId parameter is ignored for customers Service always uses the logged-in customer's ID
}