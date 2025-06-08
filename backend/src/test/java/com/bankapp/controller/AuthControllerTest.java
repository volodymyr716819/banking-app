// File: src/test/java/com/bankapp/controller/AuthControllerTest.java

package com.bankapp.controller;

import com.bankapp.model.User;
import com.bankapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "auth_user@example.com";
        testPassword = "securePassword123";
    }

    @Test
    void testLoginFailsIfUserNotApproved() throws Exception {
        User user = new User();
        user.setEmail(testEmail);
        user.setPassword(testPassword);
        user.setName("Login Not Approved");
        userRepository.save(user); // unapproved by default

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value("Invalid email or password."));
    }

    @Test
void testLoginSuccessAfterApproval() throws Exception {
    // Step 1: Register
    User user = new User();
    user.setEmail("login@test.com");
    user.setPassword("pass123");
    user.setName("Test User");

    mockMvc.perform(post("/api/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(user)))
            .andExpect(status().isOk());

    // Step 2: Approve manually (repo injection needed)
    User created = userRepository.findByEmail("login@test.com").orElseThrow();
    created.setApproved(true);
    userRepository.save(created);

    // Step 3: Login
    User creds = new User();
    creds.setEmail("login@test.com");
    creds.setPassword("pass123");

    mockMvc.perform(post("/api/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(creds)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").exists());
}
}