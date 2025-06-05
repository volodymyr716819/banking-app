package com.bankapp.controller;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

import com.bankapp.BankingBackendApplication;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(classes = BankingBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private String testEmail;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testEmail = "acc_user@test.com";
        testPassword = "password";
    }

    private void registerUser(String email, String password) throws Exception {
        User user = new User();
        user.setName("Test User");
        user.setEmail(email);
        user.setPassword(password);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk());
    }

    private Map<String, Object> loginUser(String email, String password) throws Exception {
        User creds = new User();
        creds.setEmail(email);
        creds.setPassword(password);
        String json = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readValue(json, Map.class);
    }

    private <T extends org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder> T auth(T builder,
            String token) {
        return (T) builder.header("Authorization", "Bearer " + token);
    }

    @Test
    void testSuccessfulAccountCreation() throws Exception {
        registerUser(testEmail, testPassword);
        Map<String, Object> login = loginUser(testEmail, testPassword);
        String token = (String) login.get("token");
        Long userId = ((Number) login.get("id")).longValue();

        mockMvc.perform(auth(post("/api/accounts/create")
                .param("userId", userId.toString())
                .param("type", "CHECKING"), token))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Account created")));

        assertThat(accountRepository.findByUserId(userId)).hasSize(1);
    }

    @Test
    void testSuccessfulAccountRetrieval() throws Exception {
        registerUser(testEmail, testPassword);
        Map<String, Object> login = loginUser(testEmail, testPassword);
        String token = (String) login.get("token");
        Long userId = ((Number) login.get("id")).longValue();

        mockMvc.perform(auth(post("/api/accounts/create")
                .param("userId", userId.toString())
                .param("type", "CHECKING"), token))
                .andExpect(status().isOk());

        mockMvc.perform(auth(get("/api/accounts/user/" + userId), token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("CHECKING"));
    }

    @Test
    void testInvalidAccountCreationMissingType() throws Exception {
        registerUser(testEmail, testPassword);
        Map<String, Object> login = loginUser(testEmail, testPassword);
        String token = (String) login.get("token");
        Long userId = ((Number) login.get("id")).longValue();

        mockMvc.perform(auth(post("/api/accounts/create")
                .param("userId", userId.toString()), token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUnauthorizedAccountAccess() throws Exception {
        registerUser(testEmail, testPassword);
        Map<String, Object> login = loginUser(testEmail, testPassword);
        Long userId = ((Number) login.get("id")).longValue();

        mockMvc.perform(get("/api/accounts/user/" + userId)
                .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }
}