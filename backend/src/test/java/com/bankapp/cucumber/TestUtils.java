package com.bankapp.cucumber;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for test helper methods
 */
public class TestUtils {

    /**
     * Create a test user in the database
     * 
     * @param name User name
     * @param email User email
     * @param password Raw password (will be encoded)
     * @param role User role (CUSTOMER or EMPLOYEE)
     * @param approved Whether the user is approved
     * @param userRepository Repository to save the user
     * @param passwordEncoder Password encoder
     * @return Created user entity
     */
    public static User createTestUser(
            String name, String email, String password, String role, boolean approved,
            UserRepository userRepository, PasswordEncoder passwordEncoder) {
        
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setApproved(approved);
        return userRepository.save(user);
    }
    
    /**
     * Create a test account in the database
     * 
     * @param user Owner of the account
     * @param type Account type (CHECKING or SAVINGS)
     * @param balance Initial balance
     * @param approved Whether the account is approved
     * @param closed Whether the account is closed
     * @param iban IBAN for the account
     * @param accountRepository Repository to save the account
     * @return Created account entity
     */
    public static Account createTestAccount(
            User user, String type, BigDecimal balance, boolean approved, boolean closed, String iban,
            AccountRepository accountRepository) {
        
        Account account = new Account();
        account.setUser(user);
        account.setType(type);
        account.setBalance(balance);
        account.setApproved(approved);
        account.setClosed(closed);
        account.setIban(iban);
        account.setDailyLimit(new BigDecimal("1000.00"));
        account.setAbsoluteLimit(new BigDecimal("5000.00"));
        return accountRepository.save(account);
    }
    
    /**
     * Authenticate a user and get their JWT token
     * 
     * @param email User email
     * @param password User password
     * @param mockMvc MockMvc instance
     * @param objectMapper Object mapper
     * @return JWT token for the user
     * @throws Exception If authentication fails
     */
    public static String getAuthToken(String email, String password, MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        Map<String, String> loginRequest = new HashMap<>();
        loginRequest.put("email", email);
        loginRequest.put("password", password);
        
        MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();
        
        if (result.getResponse().getStatus() != 200) {
            throw new RuntimeException("Authentication failed: " + result.getResponse().getContentAsString());
        }
        
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("token").asText();
    }
    
    /**
     * Compare BigDecimal values with a scale of 2 decimal places
     * 
     * @param expected Expected value
     * @param actual Actual value
     * @return true if the values are equal when compared with scale 2
     */
    public static boolean equalsMoney(BigDecimal expected, BigDecimal actual) {
        if (expected == null && actual == null) return true;
        if (expected == null || actual == null) return false;
        
        return expected.setScale(2, java.math.RoundingMode.HALF_UP)
                .equals(actual.setScale(2, java.math.RoundingMode.HALF_UP));
    }
    
    /**
     * Compare double values as money with 2 decimal places
     * 
     * @param expected Expected value
     * @param actual Actual value
     * @return true if the values are equal when compared with scale 2
     */
    public static boolean equalsMoney(double expected, double actual) {
        return equalsMoney(
                new BigDecimal(String.valueOf(expected)),
                new BigDecimal(String.valueOf(actual))
        );
    }
}