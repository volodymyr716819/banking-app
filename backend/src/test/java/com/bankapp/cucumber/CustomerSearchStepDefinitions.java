package com.bankapp.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:application-test.properties")
@Transactional //Each test runs in a transaction that gets rolled back this means we test with a real database (H2 in-memory)
public class CustomerSearchStepDefinitions {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private MvcResult lastResponse;
    private Map<String, User> testUsers = new HashMap<>();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        lastResponse = null;
        testUsers.clear();
        SecurityContextHolder.clearContext();
        accountRepository.deleteAll(); // Clean database
        userRepository.deleteAll();
    }

    @Given("there are test customers in the system:")
    public void thereAreTestCustomersInTheSystem(List<Map<String, String>> customers) {
        for (Map<String, String> customerData : customers) {
            // Create real user in database
            User user = new User();
            user.setName(customerData.get("name"));
            user.setEmail(customerData.get("email"));
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole(customerData.get("role"));
            
            // Set registration status from data
            String regStatus = customerData.get("registration_status");
            if ("PENDING".equals(regStatus)) {
                user.setRegistrationStatus(RegistrationStatus.PENDING);
            } else {
                user.setRegistrationStatus(RegistrationStatus.APPROVED);
            }
            
            user = userRepository.save(user);
            testUsers.put(user.getEmail(), user);
 // Create real account
            Account account = new Account();
            account.setUser(user);
            account.setType(customerData.get("account_type"));
            account.setBalance(new BigDecimal(customerData.get("balance")));
            account.setIban(customerData.get("iban"));
            account.setApproved(Boolean.parseBoolean(customerData.get("account_approved")));
            account.setClosed(false);
            account.setDailyLimit(new BigDecimal("5000.00"));
            account.setAbsoluteLimit(new BigDecimal("10000.00"));
            accountRepository.save(account);
        }
    }

    @Given("I am logged in as an employee")
    public void iAmLoggedInAsAnEmployee() {
        User employee = new User();
        employee.setName("Employee");
        employee.setEmail("employee@bank.com");
        employee.setPassword(passwordEncoder.encode("password123"));
        employee.setRole("EMPLOYEE");
        employee.setRegistrationStatus(RegistrationStatus.APPROVED);
        employee = userRepository.save(employee);
        testUsers.put(employee.getEmail(), employee);
// Set up Spring Security context (simulate logged-in user)
        setupSecurityContext("employee@bank.com", "EMPLOYEE");
    }

    @When("I search for customers with empty name")
    public void iSearchForCustomersWithEmptyName() throws Exception {
        lastResponse = mockMvc.perform(get("/api/users/search")
                .param("name", "")
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    @When("I search for customers by name {string}")
    public void iSearchForCustomersByName(String name) throws Exception {
        // Make real HTTP request to the controller
        lastResponse = mockMvc.perform(get("/api/users/search")
                .param("name", name)
                .contentType(MediaType.APPLICATION_JSON))
                .andReturn();
    }

    @Then("the search should fail with status {int}")
    public void theSearchShouldFailWithStatus(int expectedStatus) throws Exception {
        assertEquals(expectedStatus, lastResponse.getResponse().getStatus());
    }

    @Then("the search should be successful")
    public void theSearchShouldBeSuccessful() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
    }

    @And("the search results should contain customer {string}")
    public void theSearchResultsShouldContainCustomer(String customerName) throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode results = objectMapper.readTree(responseBody);
        
        boolean found = false;
        if (results.isArray()) {
            for (JsonNode result : results) {
                if (result.has("name") && customerName.equals(result.get("name").asText())) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "Customer '" + customerName + "' not found in results");
    }

    @And("the search results should not include any IBANs")
    public void theSearchResultsShouldNotIncludeAnyIbans() throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode results = objectMapper.readTree(responseBody);
        
        if (results.isArray()) {
            for (JsonNode result : results) {
                if (result.has("ibans")) {
                    JsonNode ibans = result.get("ibans");
                    assertTrue(ibans.isArray() && ibans.size() == 0,
                        "Expected no IBANs but found: " + ibans.toString());
                }
            }
        }
    }

    @And("the search results should be empty")
    public void theSearchResultsShouldBeEmpty() throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode results = objectMapper.readTree(responseBody);
        
        assertTrue(results.isArray(), "Results should be an array");
        assertEquals(0, results.size(), "Expected empty results");
    }

    @And("the search results should include IBAN {string}")
    public void theSearchResultsShouldIncludeIban(String expectedIban) throws Exception {
        String responseBody = lastResponse.getResponse().getContentAsString();
        JsonNode results = objectMapper.readTree(responseBody);
        
        boolean ibanFound = false;
        // Loop through results array
        if (results.isArray()) {
            for (JsonNode result : results) {
                // Check ibans array in each result
                if (result.has("ibans")) {
                    JsonNode ibans = result.get("ibans");
                    for (JsonNode iban : ibans) {
                        if (expectedIban.equals(iban.asText())) {
                            ibanFound = true;
                            break;
                        }
                    }
                }
            }
        }
        assertTrue(ibanFound, "Expected IBAN " + expectedIban + " not found in results");
    }

    private void setupSecurityContext(String email, String role) {
        User user = testUsers.get(email);
        String roleWithPrefix = "ROLE_" + role.toUpperCase();
        
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(user.getPassword())
                .authorities(java.util.Collections.singletonList(new SimpleGrantedAuthority(roleWithPrefix)))
                .build();

        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
    }
}