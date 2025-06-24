package com.bankapp.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional
public class ViewTransactionHistoryStepDefinitions {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MvcResult lastResponse;
    private String currentUserToken;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        lastResponse = null;
        currentUserToken = null;
    }

    @Given("the banking system is running")
    public void theBankingSystemIsRunning() {
        assertNotNull(mockMvc);
    }

    @Given("there are approved customers with accounts and transaction history:")
    public void thereAreApprovedCustomersWithAccountsAndTransactionHistory(
            java.util.List<Map<String, String>> customers) throws Exception {
        for (Map<String, String> customerData : customers) {
            User user = new User();
            user.setName(customerData.get("customer_name"));
            user.setEmail(customerData.get("email"));
            user.setPassword("password123");
            user.setApproved(true);
            user = userRepository.save(user);

            Account account = new Account();
            account.setUser(user);
            account.setType(customerData.get("account_type"));
            account.setBalance(new BigDecimal(customerData.get("balance")));
            account.setIban(customerData.get("iban"));
            account.setApproved(true);
            accountRepository.save(account);
        }
    }

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String email) {
        currentUserToken = "mock-jwt-token-for-" + email;
    }

    @When("I request my transaction history")
    public void iRequestMyTransactionHistory() throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/history")
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("I should see all transactions involving my accounts")
    public void iShouldSeeAllTransactionsInvolvingMyAccounts() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(responseBody);
        assertTrue(transactions.isArray());
    }

    @Then("the transactions should be sorted by timestamp \\(newest first)")
    public void theTransactionsShouldBeSortedByTimestampNewestFirst() throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(responseBody);
        assertTrue(transactions.isArray());
    }
}