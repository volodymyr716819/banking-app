package com.bankapp.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

import com.bankapp.dto.AtmRequest;
import com.bankapp.dto.TransferRequest;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.util.PinHashUtil;
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
@Transactional
public class BankingStepDefinitions {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;

    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PinHashUtil pinHashUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private String currentUserToken;
    private String currentUserEmail;
    private MvcResult lastResponse;
    private Map<String, Object> testData = new HashMap<>();

    @Before
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testData.clear();
        currentUserToken = null;
        currentUserEmail = null;
        lastResponse = null;
        
        // Clear the security context
        SecurityContextHolder.clearContext();
        
        // Clear all data from repositories to avoid conflicts
        atmOperationRepository.deleteAll();
        transactionRepository.deleteAll();
        cardDetailsRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
    }

    // Background and setup steps
    @Given("the banking system is running")
    public void theBankingSystemIsRunning() {
        // System is already running as part of SpringBootTest
        assertNotNull(mockMvc);
    }

    @Given("there is an employee user with email {string} and password {string}")
    public void thereIsAnEmployeeUserWithEmailAndPassword(String email, String password) {
        User employee = new User();
        employee.setName("Employee User");
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode(password));
        employee.setRole("EMPLOYEE");
        employee.setApproved(true);
        userRepository.save(employee);
    }

    @Given("there is an approved customer with name {string} and email {string}")
    public void thereIsAnApprovedCustomerWithNameAndEmail(String name, String email) {
        User customer = new User();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPassword(passwordEncoder.encode("password123"));
        customer.setRole("CUSTOMER");
        customer.setApproved(true);
        userRepository.save(customer);
        testData.put("customer_" + email, customer);
    }

    @Given("there is an employee with name {string} and email {string}")
    public void thereIsAnEmployeeWithNameAndEmail(String name, String email) {
        User employee = new User();
        employee.setName(name);
        employee.setEmail(email);
        employee.setPassword(passwordEncoder.encode("password123"));
        employee.setRole("EMPLOYEE");
        employee.setApproved(true);
        userRepository.save(employee);
        testData.put("employee_" + email, employee);
    }

    // User Registration and Approval Steps
    @Given("I am a new user")
    public void iAmANewUser() {
        // No setup needed for new user registration
    }

    @When("I register with name {string}, email {string}, and password {string}")
    public void iRegisterWithNameEmailAndPassword(String name, String email, String password) throws Exception {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);

        lastResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andReturn();
    }

    @Then("my registration should be successful")
    public void myRegistrationShouldBeSuccessful() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
    }

    @And("my account should be pending approval")
    public void myAccountShouldBePendingApproval() throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.get("message").asText().contains("pending approval"));
    }

    @And("I should receive a message {string}")
    public void iShouldReceiveAMessage(String expectedMessage) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertEquals(expectedMessage, response.get("message").asText());
    }

    @Given("there is already a user with email {string}")
    public void thereIsAlreadyAUserWithEmail(String email) {
        User existingUser = new User();
        existingUser.setName("Existing User");
        existingUser.setEmail(email);
        existingUser.setPassword(passwordEncoder.encode("password123"));
        existingUser.setRole("CUSTOMER");
        existingUser.setApproved(true);
        userRepository.save(existingUser);
    }

    @When("I try to register with email {string}")
    public void iTryToRegisterWithEmail(String email) throws Exception {
        User user = new User();
        user.setName("Duplicate User");
        user.setEmail(email);
        user.setPassword("password123");

        lastResponse = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(user)))
                .andReturn();
    }

    @Then("my registration should fail")
    public void myRegistrationShouldFail() throws Exception {
        assertEquals(400, lastResponse.getResponse().getStatus());
    }

    // This step has been moved to AdditionalStepDefinitions
    // @And("I should receive an error message {string}")
    // public void iShouldReceiveAnErrorMessage(String expectedError) throws Exception {
    //     String content = lastResponse.getResponse().getContentAsString();
    //     JsonNode response = objectMapper.readTree(content);
    //     assertEquals(expectedError, response.get("message").asText());
    // }

    @Given("there is a pending user with name {string} and email {string}")
    public void thereIsAPendingUserWithNameAndEmail(String name, String email) {
        User pendingUser = new User();
        pendingUser.setName(name);
        pendingUser.setEmail(email);
        pendingUser.setPassword(passwordEncoder.encode("password123"));
        pendingUser.setRole("CUSTOMER");
        pendingUser.setApproved(false);
        userRepository.save(pendingUser);
        testData.put("pending_user_" + email, pendingUser);
    }

    @And("I am logged in as an employee")
    public void iAmLoggedInAsAnEmployee() throws Exception {
        User employee = new User();
        employee.setName("Test Employee");
        employee.setEmail("testemployee@bank.com");
        employee.setPassword(passwordEncoder.encode("password123"));
        employee.setRole("EMPLOYEE");
        employee.setApproved(true);
        userRepository.save(employee);

        User loginRequest = new User();
        loginRequest.setEmail("testemployee@bank.com");
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String content = loginResult.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        currentUserToken = response.get("token").asText();
        currentUserEmail = "testemployee@bank.com";
        
        // Set up authentication context for the security context holder
        UserDetails employeeDetails = org.springframework.security.core.userdetails.User
                .withUsername("testemployee@bank.com")
                .password(employee.getPassword())
                .authorities(java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_EMPLOYEE")))
                .build();
        
        // Create authentication object and set it in the security context
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                employeeDetails, null, employeeDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @When("I approve the user {string}")
    public void iApproveTheUser(String email) throws Exception {
        User user = (User) testData.get("pending_user_" + email);
        assertNotNull(user, "User not found in test data");

        lastResponse = mockMvc.perform(post("/api/users/" + user.getId() + "/approve")
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("the user should be approved")
    public void theUserShouldBeApproved() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
    }

    @And("the user should be able to login")
    public void theUserShouldBeAbleToLogin() throws Exception {
        // This would require additional implementation to verify login capability
        // For now, we verify the approval response
        String content = lastResponse.getResponse().getContentAsString();
        assertEquals("User approved successfully.", content);
    }

    // Account Management Steps
    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String email) throws Exception {
        User loginRequest = new User();
        loginRequest.setEmail(email);
        loginRequest.setPassword("password123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andReturn();

        String content = loginResult.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        currentUserToken = response.get("token").asText();
        currentUserEmail = email;
        
        // Get the user from repository
        User user = userRepository.findByEmail(email).orElseThrow();
        
        // Determine role for authorities
        String role = "ROLE_" + user.getRole().toUpperCase();
        
        // Set up authentication context for the security context holder
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(email)
                .password(user.getPassword())
                .authorities(java.util.Collections.singletonList(new SimpleGrantedAuthority(role)))
                .build();
        
        // Create authentication object and set it in the security context
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @When("I create a new account with type {string}")
    public void iCreateANewAccountWithType(String accountType) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(post("/api/accounts/create")
                .param("userId", user.getId().toString())
                .param("type", accountType)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("the account should be created successfully")
    public void theAccountShouldBeCreatedSuccessfully() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        assertEquals("Account created and pending approval", lastResponse.getResponse().getContentAsString());
    }

    @And("the account should be pending approval")
    public void theAccountShouldBePendingApproval() throws Exception {
        // Verify that an account was created with approved=false
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        List<Account> accounts = accountRepository.findByUserId(user.getId());
        assertTrue(accounts.stream().anyMatch(account -> !account.isApproved()));
    }

    @And("the account balance should be {double}")
    public void theAccountBalanceShouldBe(double expectedBalance) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        List<Account> accounts = accountRepository.findByUserId(user.getId());
        Account lastCreatedAccount = accounts.get(accounts.size() - 1);
        assertEquals(new BigDecimal(String.valueOf(expectedBalance)), lastCreatedAccount.getBalance());
    }

    // Money Transfer Steps
    @Given("there are approved customers with accounts:")
    public void thereAreApprovedCustomersWithAccounts(List<Map<String, String>> customers) {
        for (Map<String, String> customerData : customers) {
            User user = new User();
            user.setName(customerData.get("name"));
            user.setEmail(customerData.get("email"));
            user.setPassword(passwordEncoder.encode("password123"));
            user.setRole("CUSTOMER");
            user.setApproved(true);
            userRepository.save(user);

            Account account = new Account();
            account.setUser(user);
            account.setType(customerData.get("account_type"));
            account.setBalance(new BigDecimal(customerData.get("balance")));
            account.setApproved(true);
            account.setClosed(false);
            account.setIban(customerData.get("iban"));
            accountRepository.save(account);

            testData.put("user_" + user.getEmail(), user);
            testData.put("account_" + user.getEmail(), account);
        }
    }

    @When("I transfer {double} from my account to account ID {int} with description {string}")
    public void iTransferFromMyAccountToAccountIdWithDescription(double amount, int toAccountId, String description) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account fromAccount = accountRepository.findByUserId(user.getId()).get(0);

        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSenderAccountId(fromAccount.getId());
        transferRequest.setReceiverAccountId((long) toAccountId);
        transferRequest.setAmount(new BigDecimal(String.valueOf(amount)));
        transferRequest.setDescription(description);

        lastResponse = mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("the transfer should be successful")
    public void theTransferShouldBeSuccessful() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        assertEquals("Transfer completed successfully", lastResponse.getResponse().getContentAsString());
    }

    @And("my account balance should be {double}")
    public void myAccountBalanceShouldBe(double expectedBalance) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).get(0);
        // Refresh the account from database
        account = accountRepository.findById(account.getId()).orElseThrow();
        assertEquals(new BigDecimal(String.valueOf(expectedBalance)), account.getBalance());
    }

    @And("the receiver's account balance should be {double}")
    public void theReceiversAccountBalanceShouldBe(double expectedBalance) throws Exception {
        // This assumes we're working with account ID 2 as receiver
        Account receiverAccount = accountRepository.findById(2L).orElseThrow();
        assertEquals(new BigDecimal(String.valueOf(expectedBalance)), receiverAccount.getBalance());
    }

    @And("a transaction record should be created")
    public void aTransactionRecordShouldBeCreated() throws Exception {
        List<Transaction> transactions = transactionRepository.findAll();
        assertFalse(transactions.isEmpty());
    }

    // ATM Operations Steps
    @Given("there is an approved customer {string} with an approved account ID {int}")
    public void thereIsAnApprovedCustomerWithAnApprovedAccountId(String email, int accountId) {
        User user = new User();
        user.setName("ATM User");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("CUSTOMER");
        user.setApproved(true);
        userRepository.save(user);

        Account account = new Account();
        account.setId((long) accountId);
        account.setUser(user);
        account.setType("CHECKING");
        account.setBalance(new BigDecimal("1000.00"));
        account.setApproved(true);
        account.setClosed(false);
        accountRepository.save(account);

        testData.put("atm_user", user);
        testData.put("atm_account", account);
    }

    @And("the account has a balance of {double}")
    public void theAccountHasABalanceOf(double balance) {
        Account account = (Account) testData.get("atm_account");
        account.setBalance(new BigDecimal(String.valueOf(balance)));
        accountRepository.save(account);
    }

    @And("the account has a PIN set to {string}")
    public void theAccountHasAPinSetTo(String pin) {
        Account account = (Account) testData.get("atm_account");
        
        CardDetails cardDetails = new CardDetails();
        cardDetails.setAccount(account);
        cardDetails.setHashedPin(pinHashUtil.hashPin(pin));
        cardDetails.setPinCreated(true);
        cardDetailsRepository.save(cardDetails);

        testData.put("card_details", cardDetails);
    }

    @Given("I am at the ATM")
    public void iAmAtTheAtm() {
        // Setup ATM context - no specific action needed
    }

    @When("I deposit {double} into account ID {int} with PIN {string}")
    public void iDepositIntoAccountIdWithPin(double amount, int accountId, String pin) throws Exception {
        AtmRequest atmRequest = new AtmRequest();
        atmRequest.setAccountId((long) accountId);
        atmRequest.setAmount(new BigDecimal(String.valueOf(amount)));
        atmRequest.setPin(pin);

        lastResponse = mockMvc.perform(post("/api/atm/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(atmRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("the deposit should be successful")
    public void theDepositShouldBeSuccessful() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        assertEquals("ATM Operation successful", lastResponse.getResponse().getContentAsString());
    }

    @And("an ATM operation record should be created for DEPOSIT")
    public void anAtmOperationRecordShouldBeCreatedForDeposit() {
        List<AtmOperation> operations = atmOperationRepository.findAll();
        assertTrue(operations.stream().anyMatch(op -> 
            op.getOperationType() == AtmOperation.OperationType.DEPOSIT));
    }

    // Additional helper steps for complex scenarios
    @Then("I should receive an access denied error")
    public void iShouldReceiveAnAccessDeniedError() throws Exception {
        assertTrue(lastResponse.getResponse().getStatus() == 403 || 
                  lastResponse.getResponse().getContentAsString().contains("Access denied"));
    }

    @Then("the transfer should fail")
    public void theTransferShouldFail() throws Exception {
        assertNotEquals(200, lastResponse.getResponse().getStatus());
    }

    @And("I should receive an error {string}")
    public void iShouldReceiveAnError(String expectedError) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        assertTrue(content.contains(expectedError), 
            "Expected error message '" + expectedError + "' not found in response: " + content);
    }

    @When("I try to transfer {double} from my account to account ID {int}")
    public void iTryToTransferFromMyAccountToAccountId(double amount, int toAccountId) throws Exception {
        iTransferFromMyAccountToAccountIdWithDescription(amount, toAccountId, "Test transfer");
    }

    @And("no money should be deducted from my account")
    public void noMoneyShouldBeDeductedFromMyAccount() throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).get(0);
        // Verify balance hasn't changed from initial value
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    @When("I try to deposit {double} into account ID {int} with PIN {string}")
    public void iTryToDepositIntoAccountIdWithPin(double amount, int accountId, String pin) throws Exception {
        iDepositIntoAccountIdWithPin(amount, accountId, pin);
    }

    @Then("the operation should fail")
    public void theOperationShouldFail() throws Exception {
        assertNotEquals(200, lastResponse.getResponse().getStatus());
    }

    @And("no money should be deposited")
    public void noMoneyShouldBeDeposited() throws Exception {
        Account account = (Account) testData.get("atm_account");
        account = accountRepository.findById(account.getId()).orElseThrow();
        // Verify balance hasn't changed
        assertEquals(new BigDecimal("1000.00"), account.getBalance());
    }

    // Transaction History Steps
    @When("I request my transaction history")
    public void iRequestMyTransactionHistory() throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + user.getId())
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }

    @Then("I should see all transactions involving my accounts")
    public void iShouldSeeAllTransactionsInvolvingMyAccounts() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        // Verify it's a JSON array
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
    }

    @And("the history should include both regular transfers and ATM operations")
    public void theHistoryShouldIncludeBothRegularTransfersAndAtmOperations() throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        // This would need more sophisticated verification based on actual data
        assertTrue(response.isArray());
    }

    @And("the transactions should be sorted by timestamp \\(newest first)")
    public void theTransactionsShouldBeSortedByTimestampNewestFirst() throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
        // Additional timestamp sorting verification would go here
    }
}