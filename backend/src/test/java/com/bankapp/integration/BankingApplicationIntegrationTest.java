package com.bankapp.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.bankapp.config.TestSecurityConfig;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.security.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
@Import(TestSecurityConfig.class)
@DisplayName("Banking Application Integration Tests")
public class BankingApplicationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private JwtUtil jwtUtil;

    private User testCustomer;
    private User testEmployee;
    private Account customerAccount;
    private String customerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders
                .webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
                
        // Clear existing users first to avoid conflicts
        userRepository.deleteAll();
        accountRepository.deleteAll();
        
        // Create test customer
        testCustomer = new User();
        testCustomer.setName("Test Customer");
        testCustomer.setEmail("customer@test.com");
        testCustomer.setPassword(passwordEncoder.encode("password123"));
        testCustomer.setRole("CUSTOMER");
        testCustomer.setApproved(true);
        userRepository.save(testCustomer);

        // Create test employee
        testEmployee = new User();
        testEmployee.setName("Test Employee");
        testEmployee.setEmail("employee@test.com");
        testEmployee.setPassword(passwordEncoder.encode("password123"));
        testEmployee.setRole("EMPLOYEE");
        testEmployee.setApproved(true);
        userRepository.save(testEmployee);

        // Create customer account
        customerAccount = new Account();
        customerAccount.setUser(testCustomer);
        customerAccount.setType("CHECKING");
        customerAccount.setBalance(new BigDecimal("1000.00"));
        customerAccount.setApproved(true);
        customerAccount.setClosed(false);
        accountRepository.save(customerAccount);

        // Set up authentication for tests using our test JWT utility
        customerToken = "test-token-customer";
        employeeToken = "test-token-employee";
        
        // Set up authentication context for the current thread
        UserDetails customerDetails = org.springframework.security.core.userdetails.User
                .withUsername("customer@test.com")
                .password(testCustomer.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
        
        // Set up the security context with the customer authentication
        UsernamePasswordAuthenticationToken authentication = 
                new UsernamePasswordAuthenticationToken(customerDetails, null, customerDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    // Simplified login for testing
    private String loginUser(String email, String password) {
        // For testing, we don't need to actually call the login endpoint
        // since we're generating tokens directly
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(password)
                .authorities(Collections.singletonList(
                    new SimpleGrantedAuthority("ROLE_" + 
                        (email.contains("employee") ? "EMPLOYEE" : "CUSTOMER"))))
                .build();
        
        return jwtUtil.generateToken(userDetails);
    }
    
    // Helper method to create user details for token generation
    private UserDetails createUserDetails(User user) {
        String role = user.getRole().toUpperCase();
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPassword())
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }

    @Test
    @DisplayName("Complete user registration and approval workflow")
    void completeUserRegistrationAndApprovalWorkflow() throws Exception {
        // 1. Register new user
        Map<String, String> newUserRequest = new HashMap<>();
        newUserRequest.put("name", "New User");
        newUserRequest.put("email", "newuser@test.com");
        newUserRequest.put("password", "password123");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newUserRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Registration successful. Your account is pending approval."));

        // 2. Verify user cannot login before approval
        Map<String, String> loginAttempt = new HashMap<>();
        loginAttempt.put("email", "newuser@test.com");
        loginAttempt.put("password", "password123");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isForbidden());

        // 3. Employee views pending users
        mockMvc.perform(get("/api/users/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].email").value("newuser@test.com"));

        // 4. Employee approves user
        User savedUser = userRepository.findByEmail("newuser@test.com").orElseThrow();
        mockMvc.perform(post("/api/users/" + savedUser.getId() + "/approve")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        // 5. User can now login successfully
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginAttempt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists());
    }

    @Test
    @DisplayName("Complete account creation and management workflow")
    void completeAccountCreationAndManagementWorkflow() throws Exception {
        // 1. Customer creates new account
        mockMvc.perform(post("/api/accounts/create")
                .param("userId", testCustomer.getId().toString())
                .param("type", "SAVINGS")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Account created and pending approval"));

        // 2. Employee views pending accounts
        mockMvc.perform(get("/api/accounts/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));

        // 3. Employee approves account
        Account pendingAccount = accountRepository.findByApprovedFalse().get(0);
        mockMvc.perform(put("/api/accounts/" + pendingAccount.getId() + "/approve")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(content().string("Account approved successfully."));

        // 4. Customer views their accounts
        mockMvc.perform(get("/api/accounts/user/" + testCustomer.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)); // Original + new account
    }

    @Test
    @DisplayName("Complete money transfer workflow")
    void completeMoneyTransferWorkflow() throws Exception {
        // 1. Create second customer and account for transfer
        User secondCustomer = new User();
        secondCustomer.setName("Second Customer");
        secondCustomer.setEmail("customer2@test.com");
        secondCustomer.setPassword(passwordEncoder.encode("password123"));
        secondCustomer.setRole("CUSTOMER");
        secondCustomer.setApproved(true);
        userRepository.save(secondCustomer);

        Account secondAccount = new Account();
        secondAccount.setUser(secondCustomer);
        secondAccount.setType("SAVINGS");
        secondAccount.setBalance(new BigDecimal("500.00"));
        secondAccount.setApproved(true);
        secondAccount.setClosed(false);
        accountRepository.save(secondAccount);

        // 2. Perform transfer
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSenderAccountId(customerAccount.getId());
        transferRequest.setReceiverAccountId(secondAccount.getId());
        transferRequest.setAmount(new BigDecimal("200.00"));
        transferRequest.setDescription("Test transfer");

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        // 3. Verify balances changed
        // Refresh accounts from database to get updated values
        Account updatedSenderAccount = accountRepository.findById(customerAccount.getId()).orElseThrow();
        Account updatedReceiverAccount = accountRepository.findById(secondAccount.getId()).orElseThrow();
        
        // Compare with scale and rounding mode considerations
        assertEquals(0, new BigDecimal("800.00").compareTo(updatedSenderAccount.getBalance().setScale(2)));
        assertEquals(0, new BigDecimal("700.00").compareTo(updatedReceiverAccount.getBalance().setScale(2)));

        // 4. Check transaction history - simplified
        mockMvc.perform(get("/api/transactions/account/" + customerAccount.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Security and authorization workflow")
    void securityAndAuthorizationWorkflow() throws Exception {
        // 1. Unauthenticated requests should fail
        mockMvc.perform(get("/api/accounts/pending"))
                .andExpect(status().isForbidden()); // Spring Security returns 403 by default

        mockMvc.perform(get("/api/users/pending"))
                .andExpect(status().isForbidden());

        // 2. Customer trying to access employee endpoints should fail
        mockMvc.perform(get("/api/accounts/pending")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/users/pending")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 3. Customer trying to access other customer's data should fail
        User otherCustomer = new User();
        otherCustomer.setName("Other Customer");
        otherCustomer.setEmail("other@test.com");
        otherCustomer.setPassword(passwordEncoder.encode("password123"));
        otherCustomer.setRole("CUSTOMER");
        otherCustomer.setApproved(true);
        userRepository.save(otherCustomer);

        mockMvc.perform(get("/api/accounts/user/" + otherCustomer.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isForbidden());

        // 4. Employee should have access to all endpoints
        mockMvc.perform(get("/api/accounts/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/accounts/user/" + testCustomer.getId())
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Error handling and validation workflow")
    void errorHandlingAndValidationWorkflow() throws Exception {
        // 1. Invalid transfer amount
        TransferRequest invalidTransfer = new TransferRequest();
        invalidTransfer.setSenderAccountId(customerAccount.getId());
        invalidTransfer.setReceiverAccountId(999L); // Non-existent account
        invalidTransfer.setAmount(new BigDecimal("100.00"));
        invalidTransfer.setDescription("Invalid transfer");

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTransfer))
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Sender or receiver account not found"));

        // 2. Insufficient balance transfer
        TransferRequest insufficientTransfer = new TransferRequest();
        insufficientTransfer.setSenderAccountId(customerAccount.getId());
        insufficientTransfer.setReceiverAccountId(customerAccount.getId()); // Self transfer
        insufficientTransfer.setAmount(new BigDecimal("2000.00")); // More than balance
        insufficientTransfer.setDescription("Insufficient balance test");

        mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(insufficientTransfer))
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Sender has insufficient balance"));

        // 3. Invalid account creation
        mockMvc.perform(post("/api/accounts/create")
                .param("userId", "999") // Non-existent user
                .param("type", "CHECKING")
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("User not found"));

        // 4. Invalid login credentials
        User invalidLogin = new User();
        invalidLogin.setEmail("invalid@test.com");
        invalidLogin.setPassword("wrongpassword");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
    @DisplayName("System performance and load handling")
    void systemPerformanceAndLoadHandling() throws Exception {
        // Create multiple accounts and transactions to test system handling
        for (int i = 0; i < 5; i++) {
            // Create multiple accounts - limit to 5 to avoid overwhelming the test
            mockMvc.perform(post("/api/accounts/create")
                    .param("userId", testCustomer.getId().toString())
                    .param("type", i % 2 == 0 ? "CHECKING" : "SAVINGS")
                    .header("Authorization", "Bearer " + customerToken))
                    .andExpect(status().isOk());
        }

        // Verify accounts created
        mockMvc.perform(get("/api/accounts/user/" + testCustomer.getId())
                .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        // Employee should see pending accounts
        mockMvc.perform(get("/api/accounts/pending")
                .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}