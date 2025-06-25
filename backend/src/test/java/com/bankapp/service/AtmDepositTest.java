package com.bankapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;

import com.bankapp.model.AtmOperation.OperationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;

@ExtendWith(MockitoExtension.class)
class AtmDepositTest {

    @Mock private AccountRepository accountRepository;
    @Mock private CardDetailsRepository cardDetailsRepository;
    @Mock private AtmOperationRepository atmOperationRepository;
    @Mock private PinHashUtil pinHashUtil;

    @InjectMocks
    private AtmService atmService;

    private Account account;
    private CardDetails cardDetails;

    @BeforeEach
    void setUp() {
        // Setup a dummy user
        User user = new User();
        user.setId(1L);
        user.setName("John Smith");
        user.setApproved(true);

        // Setup a dummy account linked to the user
        account = new Account();
        account.setId(1L);
        account.setUser(user);
        account.setType("CHECKING");
        account.setBalance(new BigDecimal("1000.00"));
        account.setApproved(true);
        account.setClosed(false);

        // Setup dummy card details for the account
        cardDetails = new CardDetails();
        cardDetails.setId(1L);
        cardDetails.setAccount(account);
        cardDetails.setHashedPin("hashedPin1234");
        cardDetails.setPinCreated(true);
    }

    @Test
    @DisplayName("Should process ATM deposit successfully")
    void shouldProcessAtmDepositSuccessfully() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        char[] pin = "1234".toCharArray();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));                                  // return test account when searched by ID
        when(cardDetailsRepository.findByAccountId(1L)).thenReturn(Optional.of(cardDetails));                  // return test card details when searched by account ID
        when(pinHashUtil.verifyPin(eq(new String(pin)), eq(cardDetails.getHashedPin()))).thenReturn(true);    // simulate PIN verification success
        when(accountRepository.save(any(Account.class))).thenReturn(account);                                // simulate saving account and returning the same account
        when(atmOperationRepository.save(any(AtmOperation.class))).thenAnswer(i -> i.getArgument(0));       // simulate saving ATM operation and returning it

        // Call the actual method to test deposit
        AtmOperation result = atmService.performAtmOperation(1L, depositAmount, pin, OperationType.DEPOSIT);

        // Assertions to verify the expected results
        assertNotNull(result);
        assertEquals(OperationType.DEPOSIT, result.getOperationType());     // Operation type should be deposit
        assertEquals(depositAmount, result.getAmount());                   // Amount should match deposit amount
        assertEquals(new BigDecimal("1500.00"), account.getBalance());    // Account balance should increase by deposit
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void shouldThrowExceptionWhenAccountNotFound() {
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());  // no account found for ID 99

        // Expect ResourceNotFoundException when calling method with invalid account ID
        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            atmService.performAtmOperation(99L, new BigDecimal("500.00"), "1234".toCharArray(), OperationType.DEPOSIT);
        });

        assertEquals("Account not found with id : '99'", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when PIN verification fails")
    void shouldThrowExceptionWhenPinVerificationFails() {
        char[] incorrectPin = "9999".toCharArray();

        // account and card details found
        when(accountRepository.findById(1L)).thenReturn(Optional.of(account));   
        when(cardDetailsRepository.findByAccountId(1L)).thenReturn(Optional.of(cardDetails));
        
        // PIN verification fails for incorrect PIN
        when(pinHashUtil.verifyPin(eq(new String(incorrectPin)), eq(cardDetails.getHashedPin()))).thenReturn(false);   

        // Expect InvalidPinException when PIN is wrong
        InvalidPinException exception = assertThrows(InvalidPinException.class, () -> {
            atmService.performAtmOperation(1L, new BigDecimal("500.00"), incorrectPin, OperationType.DEPOSIT);
        });

        assertEquals("Invalid PIN provided", exception.getMessage());
    }
}
