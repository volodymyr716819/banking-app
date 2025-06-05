package com.bankapp.integration;

import com.bankapp.BankingBackendApplication;
import com.bankapp.dto.AtmRequest;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.CardDetails;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
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

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PinHashUtil pinHashUtil;

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

    // ---------------------- Helper methods ----------------------
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

    private void deposit(Long accountId, BigDecimal amount, String pin, String token) throws Exception {
        AtmRequest req = new AtmRequest();
        req.setAccountId(accountId);
        req.setAmount(amount);
        req.setPin(pin);

        mockMvc.perform(post("/api/atm/deposit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private void transfer(Long senderId, Long receiverId, BigDecimal amount, String token) throws Exception {
        TransferRequest req = new TransferRequest();
        req.setSenderAccountId(senderId);
        req.setReceiverAccountId(receiverId);
        req.setAmount(amount);
        req.setDescription("test transfer");

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    // ---------------------- Tests ----------------------

    @Test
    void testUserRegistration() throws Exception {
        User u = new User();
        u.setName("Jane Doe");
        u.setEmail("jane.doe@test.com");
        u.setPassword("secret");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        assertThat(userRepository.findByEmail("jane.doe@test.com")).isPresent();
    }

    @Test
    void testUserRegistration_FailureDuplicateEmail() throws Exception {
        User u = new User();
        u.setName("Dup");
        u.setEmail(userEmail);
        u.setPassword("secret");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUserLogin() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        assertThat(token).isNotBlank();
    }

    @Test
    void testUserLogin_FailureInvalidPassword() throws Exception {
        User creds = new User();
        creds.setEmail(userEmail);
        creds.setPassword("wrong");

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testAccountCreation() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long accountId = createAccountForUser(approvedUser.getId(), token);
        assertThat(accountRepository.findById(accountId)).isPresent();
    }

    @Test
    void testAccountCreation_FailureUserNotFound() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        mockMvc.perform(post("/api/accounts/create")
                .param("userId", "999")
                .param("type", "CHECKING")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testDepositMoney() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long accId = createAccountForUser(approvedUser.getId(), token);
        Account acc = accountRepository.findById(accId).orElseThrow();
        acc.setApproved(true);
        accountRepository.save(acc);
        createCardDetails(accId, "1234");

        deposit(accId, new BigDecimal("100.00"), "1234", token);

        assertThat(accountRepository.findById(accId).get().getBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void testDepositMoney_FailureUnapprovedAccount() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long accId = createAccountForUser(approvedUser.getId(), token);
        // account not approved
        createCardDetails(accId, "1234");

        AtmRequest req = new AtmRequest();
        req.setAccountId(accId);
        req.setAmount(new BigDecimal("50.00"));
        req.setPin("1234");

        mockMvc.perform(post("/api/atm/deposit")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testMoneyTransfer() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long senderId = createAccountForUser(approvedUser.getId(), token);
        Long receiverId = createAccountForUser(approvedUser.getId(), token);
        Account sender = accountRepository.findById(senderId).orElseThrow();
        sender.setApproved(true);
        sender.setBalance(new BigDecimal("200.00"));
        accountRepository.save(sender);
        Account receiver = accountRepository.findById(receiverId).orElseThrow();
        receiver.setApproved(true);
        accountRepository.save(receiver);

        transfer(senderId, receiverId, new BigDecimal("50.00"), token);

        assertThat(accountRepository.findById(senderId).get().getBalance()).isEqualByComparingTo("150.00");
        assertThat(accountRepository.findById(receiverId).get().getBalance()).isEqualByComparingTo("50.00");
    }

    @Test
    void testMoneyTransfer_FailureInsufficientFunds() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long senderId = createAccountForUser(approvedUser.getId(), token);
        Long receiverId = createAccountForUser(approvedUser.getId(), token);
        Account sender = accountRepository.findById(senderId).orElseThrow();
        sender.setApproved(true);
        sender.setBalance(new BigDecimal("10.00"));
        accountRepository.save(sender);
        Account receiver = accountRepository.findById(receiverId).orElseThrow();
        receiver.setApproved(true);
        accountRepository.save(receiver);

        TransferRequest req = new TransferRequest();
        req.setSenderAccountId(senderId);
        req.setReceiverAccountId(receiverId);
        req.setAmount(new BigDecimal("100.00"));
        req.setDescription("fail");

        mockMvc.perform(post("/api/transactions/transfer")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testTransactionHistory() throws Exception {
        String token = loginAndGetToken(userEmail, userPassword);
        Long accId = createAccountForUser(approvedUser.getId(), token);
        Account acc = accountRepository.findById(accId).orElseThrow();
        acc.setApproved(true);
        accountRepository.save(acc);
        createCardDetails(accId, "1234");
        deposit(accId, new BigDecimal("20.00"), "1234", token);

        mockMvc.perform(get("/api/transactions/user/" + approvedUser.getId())
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").exists());
    }

    @Test
    void testTransactionHistory_FailureUnauthorizedAccess() throws Exception {
        // create another user and token
        User other = new User();
        other.setEmail("other@test.com");
        other.setName("Other");
        other.setPassword(passwordEncoder.encode("pass"));
        other.setRole("CUSTOMER");
        other.setRegistrationStatus(RegistrationStatus.APPROVED);
        userRepository.save(other);
        String otherToken = loginAndGetToken("other@test.com", "pass");

        Long accId = createAccountForUser(approvedUser.getId(), loginAndGetToken(userEmail, userPassword));
        Account acc = accountRepository.findById(accId).orElseThrow();
        acc.setApproved(true);
        accountRepository.save(acc);

        mockMvc.perform(get("/api/transactions/user/" + approvedUser.getId())
                .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden());
    }
}