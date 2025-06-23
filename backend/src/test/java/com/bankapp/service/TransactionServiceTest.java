// package com.bankapp.service;

// import com.bankapp.dto.TransactionHistoryDTO;
// import com.bankapp.model.Account;
// import com.bankapp.model.User;
// import com.bankapp.repository.AccountRepository;
// import com.bankapp.repository.TransactionRepository;
// import com.bankapp.repository.AtmOperationRepository;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.extension.ExtendWith;
// import org.mockito.InjectMocks;
// import org.mockito.Mock;
// import org.mockito.junit.jupiter.MockitoExtension;

// import java.util.List;
// import java.util.Optional;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.*;

// /**
//  * Unit tests for authorization and validation logic in TransactionService.
//  */
// @ExtendWith(MockitoExtension.class)
// public class TransactionServiceTest {

//     @Mock
//     private AccountRepository accountRepository;

//     @Mock
//     private TransactionRepository transactionRepository;

//     @Mock
//     private AtmOperationRepository atmOperationRepository;

//     @InjectMocks
//     private TransactionService transactionService;

//     private User employee;
//     private User customer;
//     private Account account;

//     /**
//      * Sets up reusable test objects before each test.
//      */
//     @BeforeEach
//     public void setup() {
//         employee = new User();
//         employee.setId(1L);
//         employee.setRole("EMPLOYEE");

//         customer = new User();
//         customer.setId(2L);
//         customer.setRole("CUSTOMER");

//         account = new Account();
//         account.setId(100L);
//         account.setIban("DE1234567890");
//         account.setUser(customer);
//     }

//     /**
//      * Valid: A customer can access their own account's history by IBAN.
//      */
//     @Test
//     public void testGetTransactionHistoryByIbanWithAuth_AuthorizedCustomer() {
//         when(accountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

//         List<TransactionHistoryDTO> result = transactionService.getTransactionHistoryByIbanWithAuth(account.getIban(), customer);

//         assertNotNull(result);
//         verify(accountRepository).findByIban(account.getIban());
//     }

//     /**
//      * Valid: An employee can access any account's history by IBAN.
//      */
//     @Test
//     public void testGetTransactionHistoryByIbanWithAuth_AuthorizedEmployee() {
//         when(accountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

//         List<TransactionHistoryDTO> result = transactionService.getTransactionHistoryByIbanWithAuth(account.getIban(), employee);

//         assertNotNull(result);
//         verify(accountRepository).findByIban(account.getIban());
//     }

//     /**
//      * Invalid: Another customer trying to access an account they don't own is denied.
//      */
//     @Test
//     public void testGetTransactionHistoryByIbanWithAuth_UnauthorizedUser() {
//         User otherUser = new User();
//         otherUser.setId(3L);
//         otherUser.setRole("CUSTOMER");

//         when(accountRepository.findByIban(account.getIban())).thenReturn(Optional.of(account));

//         Exception ex = assertThrows(IllegalArgumentException.class, () ->
//             transactionService.getTransactionHistoryByIbanWithAuth(account.getIban(), otherUser));

//         assertEquals("Access denied", ex.getMessage());
//     }

//     /**
//      * Valid: A user can access their own user-level transaction history.
//      */
//     @Test
//     public void testGetTransactionsByUserWithAuth_AsSelf() {
//         List<TransactionHistoryDTO> result = transactionService.getTransactionsByUserWithAuth(customer.getId(), customer);
//         assertNotNull(result);
//     }

//     /**
//      * Valid: An employee can access any user's transaction history.
//      */
//     @Test
//     public void testGetTransactionsByUserWithAuth_AsEmployee() {
//         List<TransactionHistoryDTO> result = transactionService.getTransactionsByUserWithAuth(customer.getId(), employee);
//         assertNotNull(result);
//     }

//     /**
//      * Invalid: A customer trying to access another user's history is denied.
//      */
//     @Test
//     public void testGetTransactionsByUserWithAuth_Unauthorized() {
//         User otherUser = new User();
//         otherUser.setId(3L);
//         otherUser.setRole("CUSTOMER");

//         Exception ex = assertThrows(IllegalArgumentException.class, () ->
//             transactionService.getTransactionsByUserWithAuth(customer.getId(), otherUser));

//         assertEquals("Access denied", ex.getMessage());
//     }
// }
