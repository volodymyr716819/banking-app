package com.bankapp.controller;

import com.bankapp.dto.PinRequest;
import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.PinService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PinManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    private Long accountId;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setName("PIN Tester");
        user.setEmail("test@example.com");
        user.setPassword("pass");
        user.setRole("customer");
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
        userRepository.save(user);

        Account account = new Account();
        account.setType("CHECKING");
        account.setUser(user);
        accountRepository.save(account);

        this.accountId = account.getId();
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCreatePinSuccess() throws Exception {
        PinRequest request = new PinRequest();
        request.setAccountId(accountId);
        request.setPin("1234".toCharArray());

        mockMvc.perform(post("/api/pin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("PIN created successfully"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testVerifyPinValid() throws Exception {
        // Setup: create PIN first
        PinRequest create = new PinRequest();
        create.setAccountId(accountId);
        create.setPin("1234".toCharArray());

        mockMvc.perform(post("/api/pin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        PinRequest verify = new PinRequest();
        verify.setAccountId(accountId);
        verify.setPin("1234".toCharArray());

        mockMvc.perform(post("/api/pin/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testChangePinSuccess() throws Exception {
        // Setup: create PIN first
        PinRequest create = new PinRequest();
        create.setAccountId(accountId);
        create.setPin("1234".toCharArray());

        mockMvc.perform(post("/api/pin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        PinRequest change = new PinRequest();
        change.setAccountId(accountId);
        change.setPin("1234".toCharArray());
        change.setNewPin("5678".toCharArray());

        mockMvc.perform(post("/api/pin/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(change)))
                .andExpect(status().isOk())
                .andExpect(content().string("PIN changed successfully"));
    }

    @Test
    @WithMockUser(username = "test@example.com")
    void testCheckPinStatusTrue() throws Exception {
        // Setup: create PIN first
        PinRequest create = new PinRequest();
        create.setAccountId(accountId);
        create.setPin("1234".toCharArray());

        mockMvc.perform(post("/api/pin/create")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/pin/check/" + accountId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pinCreated").value(true));
    }
}
