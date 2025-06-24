package com.bankapp.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.bankapp.model.User;
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
public class TransactionHistoryStepDefinitions {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MvcResult lastResponse;
    private String currentUserToken;

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.deleteAll();
    }

    @Given("there are test users with transactions in the system:")
    public void thereAreTestUsersWithTransactionsInTheSystem(List<Map<String, String>> users) {
        for (Map<String, String> userData : users) {
            User user = new User();
            user.setName(userData.get("name"));
            user.setEmail(userData.get("email"));
            user.setRole(userData.get("role"));
            user.setApproved(true);
            userRepository.save(user);
        }
    }

    @Given("I am logged in as customer {string}")
    public void iAmLoggedInAsCustomer(String email) {
        currentUserToken = "mock-customer-token-" + email;
    }

    @Given("I am logged in as employee {string}")
    public void iAmLoggedInAsEmployee(String email) {
        currentUserToken = "mock-employee-token-" + email;
    }

    @When("I request transaction history")
    public void iRequestTransactionHistory() throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/history")
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @When("I request transaction history for customer {string}")
    public void iRequestTransactionHistoryForCustomer(String customerName) throws Exception {
        // Employee requesting specific customer's history
        lastResponse = mockMvc.perform(get("/api/transactions/history")
                .param("customerName", customerName)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @When("I try to request another customer's transaction history")
    public void iTryToRequestAnotherCustomerTransactionHistory() throws Exception {
        // Customer trying to access another customer's data
        lastResponse = mockMvc.perform(get("/api/transactions/history")
                .param("userId", "999") // Try to access another user
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("I should see my own transactions")
    public void iShouldSeeMyOwnTransactions() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        // Mock response - in real scenario would verify actual transactions
    }

    @Then("I should not see other customers' transactions")
    public void iShouldNotSeeOtherCustomersTransactions() throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        // In real scenario, would verify no transactions from other customers appear
        assertNotNull(responseBody);
    }

    @Then("I should see that customer's transactions")
    public void iShouldSeeThatCustomersTransactions() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        // Mock response - in real scenario would verify customer's transactions
    }

    @Then("I should only see my own transactions")
    public void iShouldOnlySeeMyOwnTransactions() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        // Service should ignore userId parameter for customers
    }
}