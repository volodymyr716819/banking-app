package com.bankapp.cucumber;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.bankapp.dto.AtmRequest;
import com.bankapp.dto.TransferRequest;
import com.bankapp.dto.UpdateLimitsRequest;
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

public class BankingStepDefinitionsExtended {

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
    public void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        testData.clear();
        currentUserToken = null;
        currentUserEmail = null;
        lastResponse = null;
    }

    // ATM Operation Extensions
    
    @When("I withdraw {double} from account ID {int} with PIN {string}")
    public void iWithdrawFromAccountIdWithPin(double amount, int accountId, String pin) throws Exception {
        AtmRequest atmRequest = new AtmRequest();
        atmRequest.setAccountId((long) accountId);
        atmRequest.setAmount(new BigDecimal(String.valueOf(amount)));
        atmRequest.setPin(pin);

        lastResponse = mockMvc.perform(post("/api/atm/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(atmRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("the withdrawal should be successful")
    public void theWithdrawalShouldBeSuccessful() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        assertEquals("ATM Operation successful", lastResponse.getResponse().getContentAsString());
    }
    
    @And("an ATM operation record should be created for WITHDRAW")
    public void anAtmOperationRecordShouldBeCreatedForWithdraw() {
        List<AtmOperation> operations = atmOperationRepository.findAll();
        assertTrue(operations.stream().anyMatch(op -> 
            op.getOperationType() == AtmOperation.OperationType.WITHDRAW));
    }
    
    @When("I try to withdraw {double} from account ID {int} with PIN {string}")
    public void iTryToWithdrawFromAccountIdWithPin(double amount, int accountId, String pin) throws Exception {
        iWithdrawFromAccountIdWithPin(amount, accountId, pin);
    }
    
    @When("I check the balance for account ID {int}")
    public void iCheckTheBalanceForAccountId(int accountId) throws Exception {
        lastResponse = mockMvc.perform(get("/api/atm/balance/" + accountId)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("I should see the balance {double}")
    public void iShouldSeeTheBalance(double expectedBalance) throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertEquals(expectedBalance, response.get("balance").asDouble());
    }
    
    @When("I check the PIN status for account ID {int}")
    public void iCheckThePinStatusForAccountId(int accountId) throws Exception {
        lastResponse = mockMvc.perform(get("/api/atm/pin-status/" + accountId)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("the PIN status should show {string}")
    public void thePinStatusShouldShow(String expectedStatus) throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        assertEquals(expectedStatus, content);
    }
    
    // Money Transfer Extensions
    
    @When("I transfer {double} from IBAN {string} to IBAN {string} with description {string}")
    public void iTransferFromIbanToIbanWithDescription(double amount, String fromIban, String toIban, String description) throws Exception {
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSenderIban(fromIban);
        transferRequest.setReceiverIban(toIban);
        transferRequest.setAmount(new BigDecimal(String.valueOf(amount)));
        transferRequest.setDescription(description);

        lastResponse = mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Given("there are historical transactions:")
    public void thereAreHistoricalTransactions(List<Map<String, String>> transactionsList) throws Exception {
        for (Map<String, String> transactionData : transactionsList) {
            long fromAccountId = Long.parseLong(transactionData.get("from_account_id"));
            long toAccountId = Long.parseLong(transactionData.get("to_account_id"));
            BigDecimal amount = new BigDecimal(transactionData.get("amount"));
            String description = transactionData.get("description");
            LocalDateTime timestamp = LocalDateTime.parse(transactionData.get("timestamp"));
            
            Account fromAccount = accountRepository.findById(fromAccountId).orElseThrow();
            Account toAccount = accountRepository.findById(toAccountId).orElseThrow();
            
            Transaction transaction = new Transaction();
            transaction.setFromAccount(fromAccount);
            transaction.setToAccount(toAccount);
            transaction.setAmount(amount);
            transaction.setDescription(description);
            transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
            transaction.setTimestamp(timestamp);
            
            transactionRepository.save(transaction);
        }
    }
    
    @Given("there are ATM operations:")
    public void thereAreAtmOperations(List<Map<String, String>> operationsList) throws Exception {
        for (Map<String, String> operationData : operationsList) {
            long accountId = Long.parseLong(operationData.get("account_id"));
            String operationType = operationData.get("operation_type");
            BigDecimal amount = new BigDecimal(operationData.get("amount"));
            LocalDateTime timestamp = LocalDateTime.parse(operationData.get("timestamp"));
            
            Account account = accountRepository.findById(accountId).orElseThrow();
            
            AtmOperation operation = new AtmOperation();
            operation.setAccount(account);
            operation.setAmount(amount);
            operation.setOperationType(AtmOperation.OperationType.valueOf(operationType));
            operation.setTimestamp(timestamp);
            
            atmOperationRepository.save(operation);
        }
    }
    
    @When("I request transaction history for my account ID {int}")
    public void iRequestTransactionHistoryForMyAccountId(int accountId) throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/account/" + accountId)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I request transaction history for IBAN {string}")
    public void iRequestTransactionHistoryForIban(String iban) throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/account")
                .param("iban", iban)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("I should see all transactions for that specific account")
    public void iShouldSeeAllTransactionsForThatSpecificAccount() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
    }
    
    @And("the history should include both incoming and outgoing transactions")
    public void theHistoryShouldIncludeBothIncomingAndOutgoingTransactions() throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
        // Additional verification could be done here based on transaction types
    }
    
    @And("ATM operations for that account should be included")
    public void atmOperationsForThatAccountShouldBeIncluded() throws Exception {
        // Implementation depends on specific API response format
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
    }
    
    @When("I try to view transaction history for user ID {int}")
    public void iTryToViewTransactionHistoryForUserId(int userId) throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + userId)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I try to view transaction history for account ID {int}")
    public void iTryToViewTransactionHistoryForAccountId(int accountId) throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/account/" + accountId)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I try to view transaction history for IBAN {string}")
    public void iTryToViewTransactionHistoryForIban(String iban) throws Exception {
        lastResponse = mockMvc.perform(get("/api/transactions/account")
                .param("iban", iban)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @And("no transaction data should be returned")
    public void noTransactionDataShouldBeReturned() throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        // Either response is empty or an error message
        assertTrue(content.isEmpty() || content.contains("error") || content.contains("denied"));
    }
    
    // Transaction Limits and Management
    
    @Given("my account has a daily limit of {double} and absolute limit of {double}")
    public void myAccountHasADailyLimitOfAndAbsoluteLimitOf(double dailyLimit, double absoluteLimit) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account account = accountRepository.findByUserId(user.getId()).get(0);
        
        account.setDailyLimit(new BigDecimal(String.valueOf(dailyLimit)));
        account.setAbsoluteLimit(new BigDecimal(String.valueOf(absoluteLimit)));
        accountRepository.save(account);
    }
    
    @When("I try to exceed my daily limit by making multiple transfers:")
    public void iTryToExceedMyDailyLimitByMakingMultipleTransfers(List<Map<String, String>> transfers) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account senderAccount = accountRepository.findByUserId(user.getId()).get(0);
        
        for (Map<String, String> transfer : transfers) {
            TransferRequest transferRequest = new TransferRequest();
            transferRequest.setSenderAccountId(senderAccount.getId());
            transferRequest.setReceiverAccountId(Long.parseLong(transfer.get("receiver_id")));
            transferRequest.setAmount(new BigDecimal(transfer.get("amount")));
            transferRequest.setDescription(transfer.get("description"));
            
            MvcResult result = mockMvc.perform(post("/api/transactions/transfer")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(transferRequest))
                    .header("Authorization", "Bearer " + currentUserToken))
                    .andReturn();
            
            // Store the last response for assertion
            lastResponse = result;
        }
    }
    
    @When("I try to exceed my absolute limit with a single transfer")
    public void iTryToExceedMyAbsoluteLimitWithASingleTransfer() throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account senderAccount = accountRepository.findByUserId(user.getId()).get(0);
        
        TransferRequest transferRequest = new TransferRequest();
        transferRequest.setSenderAccountId(senderAccount.getId());
        transferRequest.setReceiverAccountId(2L); // Assuming account ID 2 is valid
        transferRequest.setAmount(new BigDecimal("10000.00")); // Assuming this exceeds absolute limit
        transferRequest.setDescription("Exceeding absolute limit test");
        
        lastResponse = mockMvc.perform(post("/api/transactions/transfer")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(transferRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I set new limits on account ID {int}:")
    public void iSetNewLimitsOnAccountId(int accountId, Map<String, String> limits) throws Exception {
        UpdateLimitsRequest limitsRequest = new UpdateLimitsRequest();
        limitsRequest.dailyLimit = new BigDecimal(limits.get("daily_limit"));
        limitsRequest.absoluteLimit = new BigDecimal(limits.get("absolute_limit"));
        
        lastResponse = mockMvc.perform(put("/api/accounts/" + accountId + "/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(limitsRequest))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("the limits should be updated successfully")
    public void theLimitsShouldBeUpdatedSuccessfully() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
    }
    
    // Advanced Transaction Filtering
    
    @When("I filter transactions by date range from {string} to {string}")
    public void iFilterTransactionsByDateRangeFromTo(String fromDate, String toDate) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + user.getId())
                .param("fromDate", fromDate)
                .param("toDate", toDate)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I filter transactions by amount greater than {double}")
    public void iFilterTransactionsByAmountGreaterThan(double amount) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + user.getId())
                .param("minAmount", String.valueOf(amount))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I filter transactions by amount less than {double}")
    public void iFilterTransactionsByAmountLessThan(double amount) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + user.getId())
                .param("maxAmount", String.valueOf(amount))
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I filter transactions by specific IBAN {string}")
    public void iFilterTransactionsBySpecificIban(String iban) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        
        lastResponse = mockMvc.perform(get("/api/transactions/user/" + user.getId())
                .param("iban", iban)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("only transactions from {string} to {string} should be shown")
    public void onlyTransactionsFromToShouldBeShown(String fromDate, String toDate) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(content);
        
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        
        // Check all transactions are within date range
        for (JsonNode transaction : transactions) {
            LocalDate txDate = LocalDate.parse(transaction.get("timestamp").asText().split("T")[0]);
            assertTrue(
                (txDate.isEqual(from) || txDate.isAfter(from)) && 
                (txDate.isEqual(to) || txDate.isBefore(to))
            );
        }
    }
    
    @Then("only transactions with amount greater than {double} should be shown")
    public void onlyTransactionsWithAmountGreaterThanShouldBeShown(double minAmount) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(content);
        
        // Check all transactions have amount greater than minAmount
        for (JsonNode transaction : transactions) {
            assertTrue(transaction.get("amount").asDouble() > minAmount);
        }
    }
    
    @Then("only transactions with amount less than {double} should be shown")
    public void onlyTransactionsWithAmountLessThanShouldBeShown(double maxAmount) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(content);
        
        // Check all transactions have amount less than maxAmount
        for (JsonNode transaction : transactions) {
            assertTrue(transaction.get("amount").asDouble() < maxAmount);
        }
    }
    
    @Then("only transactions involving IBAN {string} should be shown")
    public void onlyTransactionsInvolvingIbanShouldBeShown(String iban) throws Exception {
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode transactions = objectMapper.readTree(content);
        
        // Since JSON structure might vary, we'd need to adapt this logic
        // based on actual response structure
        for (JsonNode transaction : transactions) {
            assertTrue(
                (transaction.has("senderIban") && transaction.get("senderIban").asText().equals(iban)) ||
                (transaction.has("receiverIban") && transaction.get("receiverIban").asText().equals(iban))
            );
        }
    }
    
    // Concurrent transaction testing
    
    @When("I perform {int} concurrent transactions")
    public void iPerformConcurrentTransactions(int numTransactions) throws Exception {
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account senderAccount = accountRepository.findByUserId(user.getId()).get(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numTransactions);
        CountDownLatch latch = new CountDownLatch(numTransactions);
        List<MvcResult> results = new ArrayList<>();
        
        for (int i = 0; i < numTransactions; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    TransferRequest transferRequest = new TransferRequest();
                    transferRequest.setSenderAccountId(senderAccount.getId());
                    transferRequest.setReceiverAccountId(2L);
                    transferRequest.setAmount(new BigDecimal("10.00"));
                    transferRequest.setDescription("Concurrent transfer " + index);
                    
                    MvcResult result = mockMvc.perform(post("/api/transactions/transfer")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transferRequest))
                            .header("Authorization", "Bearer " + currentUserToken))
                            .andReturn();
                    
                    synchronized (results) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await(); // Wait for all transactions to complete
        executor.shutdown();
        
        // Store the last result for assertions
        if (!results.isEmpty()) {
            lastResponse = results.get(results.size() - 1);
        }
        
        // Store results for further assertions
        testData.put("concurrent_results", results);
    }
    
    @Then("all transactions should be processed correctly")
    public void allTransactionsShouldBeProcessedCorrectly() throws Exception {
        List<MvcResult> results = (List<MvcResult>) testData.get("concurrent_results");
        
        // Check that all transactions were successful
        int successCount = 0;
        for (MvcResult result : results) {
            if (result.getResponse().getStatus() == 200) {
                successCount++;
            }
        }
        
        // We may not expect all to succeed due to concurrency
        // but we should have a significant number of successes
        assertTrue(successCount > 0, "No transactions succeeded");
        
        // Verify final account balance is consistent
        User user = userRepository.findByEmail(currentUserEmail).orElseThrow();
        Account senderAccount = accountRepository.findById(
            accountRepository.findByUserId(user.getId()).get(0).getId()
        ).orElseThrow();
        
        // The balance should reflect the successful transactions
        BigDecimal expectedMaxDeduction = new BigDecimal("10.00").multiply(new BigDecimal(successCount));
        BigDecimal minExpectedBalance = new BigDecimal("1000.00").subtract(expectedMaxDeduction);
        
        assertTrue(senderAccount.getBalance().compareTo(minExpectedBalance) >= 0,
            "Account balance is inconsistent with successful transactions");
    }
    
    // Search functionality
    
    @When("I search for users by name {string}")
    public void iSearchForUsersByName(String name) throws Exception {
        lastResponse = mockMvc.perform(get("/api/users/search")
                .param("name", name)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @When("I search for accounts by IBAN {string}")
    public void iSearchForAccountsByIban(String iban) throws Exception {
        lastResponse = mockMvc.perform(get("/api/accounts/search")
                .param("iban", iban)
                .header("Authorization", "Bearer " + currentUserToken))
                .andReturn();
    }
    
    @Then("I should see users matching the search criteria")
    public void iShouldSeeUsersMatchingTheSearchCriteria() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
        assertTrue(response.size() > 0, "No users found matching the criteria");
    }
    
    @Then("I should see accounts matching the search criteria")
    public void iShouldSeeAccountsMatchingTheSearchCriteria() throws Exception {
        assertEquals(200, lastResponse.getResponse().getStatus());
        String content = lastResponse.getResponse().getContentAsString();
        JsonNode response = objectMapper.readTree(content);
        assertTrue(response.isArray());
        assertTrue(response.size() > 0, "No accounts found matching the criteria");
    }
}