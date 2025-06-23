// package com.bankapp.service;

// import com.bankapp.dto.UserSearchResultDTO;
// import com.bankapp.model.Account;
// import com.bankapp.model.User;
// import com.bankapp.model.enums.RegistrationStatus;
// import com.bankapp.repository.AccountRepository;
// import com.bankapp.repository.UserRepository;
// import com.bankapp.util.IbanGenerator;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.MockitoAnnotations;
// import org.springframework.security.authentication.TestingAuthenticationToken;
// import org.springframework.security.core.Authentication;

// import java.math.BigDecimal;
// import java.util.Collections;
// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.ArgumentMatchers.anyString;
// import static org.mockito.Mockito.*;

// class UserSearchServiceTest {

//     @Mock
//     private UserRepository userRepository;

//     @Mock
//     private AccountRepository accountRepository;

//     @InjectMocks
//     private UserSearchService userSearchService;

//     private Authentication authentication;

//     private User employeeUser;
//     private User customerUser;
//     private Account customerAccount;

//     @BeforeEach
//     void setUp() {
//         // Initialize mocks
//         MockitoAnnotations.openMocks(this);
        
//         // Setup employee user who will perform the search
//         employeeUser = new User();
//         employeeUser.setId(1L);
//         employeeUser.setEmail("volodymyr.gulchenko@bank.com");
//         employeeUser.setName("Volodymyr Gulchenko");
//         employeeUser.setRole("EMPLOYEE");
//         employeeUser.setRegistrationStatus(RegistrationStatus.APPROVED);

//         // Setup a customer that we will search for
//         customerUser = new User();
//         customerUser.setId(2L);
//         customerUser.setEmail("alaa.aldrobe@example.com");
//         customerUser.setName("Alaa Aldrobe");
//         customerUser.setRole("CUSTOMER");
//         customerUser.setRegistrationStatus(RegistrationStatus.APPROVED);

//         // Setup an account for the customer
//         customerAccount = new Account();
//         customerAccount.setId(101L);
//         customerAccount.setUser(customerUser);
//         customerAccount.setBalance(new BigDecimal("1000.00"));
//         customerAccount.setApproved(true);
//         customerAccount.setClosed(false);
        
//         // Create a real TestingAuthenticationToken instead of mocking
//         authentication = new TestingAuthenticationToken("volodymyr.gulchenko@bank.com", "password");
        
//         // Setup repository behavior for authentication
//         when(userRepository.findByEmail("volodymyr.gulchenko@bank.com")).thenReturn(Optional.of(employeeUser));
//     }

//     @Test
//     void testSearchUsersByName() {
//         // Setup: Mock that searching by name returns our customer
//         when(userRepository.searchApprovedByNameEmailAndRole("Alaa", null, "customer"))
//                 .thenReturn(List.of(customerUser));
        
//         // Setup: Mock the account lookup for the customer
//         when(accountRepository.findByUserId(customerUser.getId())).thenReturn(List.of(customerAccount));

//         // Action: Search for user by name
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, "Alaa", null, null, authentication);

//         // Verify: Check that we found the right user
//         assertEquals(1, results.size());
//         assertEquals("Alaa Aldrobe", results.get(0).getName());
        
//         // Verify: Check that we called the right repository method
//         verify(userRepository).searchApprovedByNameEmailAndRole("Alaa", null, "customer");
//     }

//     @Test
//     void testSearchUsersByEmail() {
//         // Setup: Mock that searching by email returns our customer
//         when(userRepository.searchApprovedByNameEmailAndRole(null, "alaa.aldrobe", "customer"))
//                 .thenReturn(List.of(customerUser));
        
//         // Setup: Mock the account lookup for the customer
//         when(accountRepository.findByUserId(customerUser.getId())).thenReturn(List.of(customerAccount));

//         // Action: Search for user by email
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, null, "alaa.aldrobe", null, authentication);

//         // Verify: Check that we found the right user
//         assertEquals(1, results.size());
//         assertEquals("Alaa Aldrobe", results.get(0).getName());
        
//         // Verify: Check that we called the right repository method
//         verify(userRepository).searchApprovedByNameEmailAndRole(null, "alaa.aldrobe", "customer");
//     }

//     @Test
//     void testSearchUsersByGenericTerm() {
//         // Setup: Mock that searching by term returns our customer
//         String searchTerm = "Alaa";
//         when(userRepository.findApprovedCustomersBySearchTerm(searchTerm)).thenReturn(List.of(customerUser));
        
//         // Setup: Mock the account lookup for the customer
//         when(accountRepository.findByUserId(customerUser.getId())).thenReturn(List.of(customerAccount));

//         // Action: Search for user by general term
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(searchTerm, null, null, null, authentication);

//         // Verify: Check that we found the right user
//         assertEquals(1, results.size());
//         assertEquals("Alaa Aldrobe", results.get(0).getName());
        
//         // Verify: Check that we called the right repository method
//         verify(userRepository).findApprovedCustomersBySearchTerm(searchTerm);
//     }

//     @Test
//     void testSearchUsersByIban() {
//         // Setup: Generate a valid IBAN for the customer's account
//         String validIban = IbanGenerator.generateIban(101L);
        
//         // Setup: Mock the account lookup by ID
//         when(accountRepository.findById(101L)).thenReturn(Optional.of(customerAccount));
        
//         // Setup: Mock the account lookup for the customer
//         when(accountRepository.findByUserId(customerUser.getId())).thenReturn(List.of(customerAccount));

//         // Action: Search for user by IBAN
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, null, null, validIban, authentication);

//         // Verify: Check that we found the right user
//         assertEquals(1, results.size());
//         assertEquals("Alaa Aldrobe", results.get(0).getName());
        
//         // Verify: Check that we called the right repository method
//         verify(accountRepository).findById(101L);
//     }
    
//     @Test
//     void testSearchUsersNoResults() {
//         // Setup: Repository returns no results for this search
//         when(userRepository.findApprovedCustomersBySearchTerm("NonExistent")).thenReturn(Collections.emptyList());

//         // Action: Search for a non-existent user
//         List<UserSearchResultDTO> results = userSearchService.searchUsers("NonExistent", null, null, null, authentication);

//         // Verify: Check that we got no results
//         assertTrue(results.isEmpty());
//     }

//     @Test
//     void testSearchUsersNoParameters() {
//         // Action & Verify: Check that searching with no parameters throws an exception
//         Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//             userSearchService.searchUsers(null, null, null, null, authentication);
//         });
        
//         // Verify the error message
//         assertEquals("At least one search parameter is required", exception.getMessage());
//     }

//     @Test
//     void testSearchUsersNotAuthenticated() {
//         // Action & Verify: Check that searching without authentication throws an exception
//         Authentication nullAuth = null;
//         Exception exception = assertThrows(IllegalArgumentException.class, () -> {
//             userSearchService.searchUsers("Alaa", null, null, null, nullAuth);
//         });
        
//         // Verify the error message
//         assertEquals("User not authenticated", exception.getMessage());
//     }

//     @Test
//     void testSearchUsersWithInvalidIban() {
//         // Setup: An invalid IBAN format
//         String invalidIban = "NL99INVALIDIBAN";

//         // Action: Search with invalid IBAN
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, null, null, invalidIban, authentication);

//         // Verify: Check that we got no results for invalid IBAN
//         assertTrue(results.isEmpty());
//     }
    
//     @Test
//     void testSearchUsersWithClosedAccount() {
//         // Setup: The account is closed
//         customerAccount.setClosed(true);
        
//         // Setup: Generate a valid IBAN for the customer's account
//         String validIban = IbanGenerator.generateIban(101L);
        
//         // Setup: Mock the account lookup by ID
//         when(accountRepository.findById(101L)).thenReturn(Optional.of(customerAccount));

//         // Action: Search by IBAN of closed account
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, null, null, validIban, authentication);

//         // Verify: Check that we don't get results for closed accounts
//         assertTrue(results.isEmpty());
//     }
    
//     @Test
//     void testSearchUsersReturnsCorrectIbans() {
//         // Setup: Create another account for the same customer
//         Account secondAccount = new Account();
//         secondAccount.setId(102L);
//         secondAccount.setUser(customerUser);
//         secondAccount.setApproved(true);
//         secondAccount.setClosed(false);
        
//         // Setup: Mock search by name returns our customer
//         when(userRepository.searchApprovedByNameEmailAndRole("Alaa", null, "customer"))
//                 .thenReturn(List.of(customerUser));
        
//         // Setup: Mock customer has two accounts
//         when(accountRepository.findByUserId(customerUser.getId())).thenReturn(List.of(customerAccount, secondAccount));

//         // Action: Search for user by name
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, "Alaa", null, null, authentication);

//         // Verify: We found the right user
//         assertEquals(1, results.size());
        
//         // Verify: The result includes both account IBANs
//         assertEquals(2, results.get(0).getIbans().size());
        
//         // Verify: IBANs match the expected format
//         for (String iban : results.get(0).getIbans()) {
//             assertTrue(iban.startsWith("NL"));
//             assertTrue(IbanGenerator.validateIban(iban));
//         }
//     }
    
//     @Test
//     void testSearchUsersByDanBreczinski() {
//         // Setup: Create a new customer user
//         User danUser = new User();
//         danUser.setId(3L);
//         danUser.setEmail("dan.breczinski@example.com");
//         danUser.setName("Dan Breczinski");
//         danUser.setRole("CUSTOMER");
//         danUser.setRegistrationStatus(RegistrationStatus.APPROVED);
        
//         // Setup: Mock search by name returns Dan
//         when(userRepository.searchApprovedByNameEmailAndRole("Dan", null, "customer"))
//                 .thenReturn(List.of(danUser));
        
//         // Setup: Dan has no accounts yet
//         when(accountRepository.findByUserId(danUser.getId())).thenReturn(Collections.emptyList());

//         // Action: Search for Dan by name
//         List<UserSearchResultDTO> results = userSearchService.searchUsers(null, "Dan", null, null, authentication);

//         // Verify: We found Dan
//         assertEquals(1, results.size());
//         assertEquals("Dan Breczinski", results.get(0).getName());
        
//         // Verify: Dan has no IBANs yet
//         assertTrue(results.get(0).getIbans().isEmpty());
//     }
// }