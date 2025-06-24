// Updated BankingApplicationIntegrationTest.java to support char[] pin and unified ATM operation

package com.bankapp.integration;

import com.bankapp.BankingBackendApplication;
import com.bankapp.dto.AtmRequest;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.CardDetails;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.model.AtmOperation.OperationType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.util.PinHashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BankingBackendApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class BankingApplicationIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PinHashUtil pinHashUtil;

    private String userEmail = "john.doe@test.com";
    private String userPassword = "password";
    private User approvedUser;

    @BeforeEach
    void setup() {
        approvedUser = new User();
        approvedUser.setEmail(userEmail);
        approvedUser.setName("John Doe");
        approvedUser.setPassword(passwordEncoder.encode(userPassword));
        approvedUser.setRole("CUSTOMER");
        approvedUser.setRegistrationStatus(RegistrationStatus.APPROVED);
        userRepository.save(approvedUser);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        User credentials = new User();
        credentials.setEmail(email);
        credentials.setPassword(password);

        String response = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(credentials)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<?, ?> map = objectMapper.readValue(response, Map.class);
        return (String) map.get("token");
    }

    private Long createAccountForUser(Long userId, String token) throws Exception {
        mockMvc.perform(post("/api/accounts/create")
                .param("userId", userId.toString())
                .param("type", "CHECKING")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        return accountRepository.findByUserId(userId).get(0).getId();
    }

    private void createCardDetails(Long accountId, String pin) {
        Account account = accountRepository.findById(accountId).orElseThrow();
        CardDetails cd = new CardDetails();
        cd.setAccount(account);
        cd.setHashedPin(pinHashUtil.hashPin(pin));
        cd.setPinCreated(true);
        cardDetailsRepository.save(cd);
    }

    private void depositUnified(BigDecimal amount, char[] pin, OperationType type, String token) throws Exception {
        AtmRequest req = new AtmRequest();
        req.setAmount(amount);
        req.setPin(pin);
        req.setOperationType(type);

        mockMvc.perform(post("/api/atm/operation")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ... All tests remain the same ... just update testDepositMoney() to use depositUnified

    @Test
    void testDepositMoney() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long accId = createAccountForUser(approvedUser.getId(), token);
        Account acc = accountRepository.findById(accId).orElseThrow();
        acc.setApproved(true);
        accountRepository.save(acc);
        createCardDetails(accId, "1234");

        depositUnified(new BigDecimal("100.00"), "1234".toCharArray(), OperationType.DEPOSIT, token);

        assertThat(accountRepository.findById(accId).get().getBalance()).isEqualByComparingTo("100.00");
    }

    // Optional: rename other deposit() usages or overload if needed
}