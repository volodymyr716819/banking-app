package com.bankapp.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;
import com.bankapp.util.IbanGenerator;

@ExtendWith(MockitoExtension.class)
@DisplayName("TransactionService Tests")
public class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AtmOperationRepository atmOperationRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User senderUser;
    private User receiverUser;
    private Account senderAccount;
    private Account receiverAccount;
    private Transaction transaction;
    private AtmOperation atmOperation;

    @BeforeEach
    void setUp() {
        // Setup users
        senderUser = new User();
        senderUser.setId(1L);
        senderUser.setName("John Doe");
        senderUser.setEmail("john@example.com");
        senderUser.setRole("CUSTOMER");
        senderUser.setApproved(true);

        receiverUser = new User();
        receiverUser.setId(2L);
        receiverUser.setName("Jane Smith");
        receiverUser.setEmail("jane@example.com");
        receiverUser.setRole("CUSTOMER");
        receiverUser.setApproved(true);

        // Setup accounts
        senderAccount = new Account();
        senderAccount.setId(1L);
        senderAccount.setUser(senderUser);
        senderAccount.setType("CHECKING");
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setApproved(true);
        senderAccount.setClosed(false);
        senderAccount.setIban("NL91ABNA0417164300");

        receiverAccount = new Account();
        receiverAccount.setId(2L);
        receiverAccount.setUser(receiverUser);
        receiverAccount.setType("SAVINGS");
        receiverAccount.setBalance(new BigDecimal("500.00"));
        receiverAccount.setApproved(true);
        receiverAccount.setClosed(false);
        receiverAccount.setIban("NL91ABNA0417164301");

        // Setup transaction
        transaction = new Transaction();
        transaction.setId(1L);
        transaction.setFromAccount(senderAccount);
        transaction.setToAccount(receiverAccount);
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setDescription("Test transfer");
        transaction.setTransactionType(Transaction.TransactionType.TRANSFER);
        transaction.setTimestamp(LocalDateTime.now());

        // Setup ATM operation
        atmOperation = new AtmOperation();
        atmOperation.setId(1L);
        atmOperation.setAccount(senderAccount);
        atmOperation.setAmount(new BigDecimal("50.00"));
        atmOperation.setOperationType(AtmOperation.OperationType.WITHDRAW);
        atmOperation.setTimestamp(LocalDateTime.now());
    }

    @Nested
    @DisplayName("Transfer Money Tests")
    class TransferMoneyTests {

        @Test
        @DisplayName("Should successfully transfer money between valid accounts")
        void shouldTransferMoneySuccessfully() {
            // Given
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            // Then
            verify(accountRepository, times(2)).save(any(Account.class));
            verify(transactionRepository, times(1)).save(any(Transaction.class));
            assertEquals(new BigDecimal("900.00"), senderAccount.getBalance());
            assertEquals(new BigDecimal("600.00"), receiverAccount.getBalance());
        }

        @Test
        @DisplayName("Should throw exception when transfer amount is zero")
        void shouldThrowExceptionWhenAmountIsZero() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, BigDecimal.ZERO, "Test transfer");
            });

            assertEquals("Amount must be greater than zero", exception.getMessage());
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when transfer amount is negative")
        void shouldThrowExceptionWhenAmountIsNegative() {
            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("-100.00"), "Test transfer");
            });

            assertEquals("Amount must be greater than zero", exception.getMessage());
            verify(accountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("Should throw exception when sender account not found")
        void shouldThrowExceptionWhenSenderAccountNotFound() {
            // Given
            when(accountRepository.findById(1L)).thenReturn(Optional.empty());
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Sender or receiver account not found", exception.getMessage());
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when receiver account not found")
        void shouldThrowExceptionWhenReceiverAccountNotFound() {
            // Given
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Sender or receiver account not found", exception.getMessage());
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when sender account is not approved")
        void shouldThrowExceptionWhenSenderAccountNotApproved() {
            // Given
            senderAccount.setApproved(false);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Sender account is not approved for transactions", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when receiver account is not approved")
        void shouldThrowExceptionWhenReceiverAccountNotApproved() {
            // Given
            receiverAccount.setApproved(false);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Receiver account is not approved for transactions", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when sender account is closed")
        void shouldThrowExceptionWhenSenderAccountClosed() {
            // Given
            senderAccount.setClosed(true);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Sender account is closed and cannot make transactions", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when receiver account is closed")
        void shouldThrowExceptionWhenReceiverAccountClosed() {
            // Given
            receiverAccount.setClosed(true);
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Receiver account is closed and cannot receive transactions", exception.getMessage());
        }

        @Test
        @DisplayName("Should throw exception when sender has insufficient balance")
        void shouldThrowExceptionWhenInsufficientBalance() {
            // Given
            senderAccount.setBalance(new BigDecimal("50.00"));
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            assertEquals("Sender has insufficient balance", exception.getMessage());
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should handle exact balance transfer")
        void shouldHandleExactBalanceTransfer() {
            // Given
            senderAccount.setBalance(new BigDecimal("100.00"));
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "Test transfer");
            });

            // Then
            assertEquals(new BigDecimal("0.00"), senderAccount.getBalance());
            assertEquals(new BigDecimal("600.00"), receiverAccount.getBalance());
        }
    }

    @Nested
    @DisplayName("Transfer Money By IBAN Tests")
    class TransferMoneyByIbanTests {

        @Test
        @DisplayName("Should successfully transfer money using valid IBANs")
        void shouldTransferMoneyByIbanSuccessfully() {
            // Given
            try (var mockedIbanGenerator = mockStatic(IbanGenerator.class)) {
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban("NL91ABNA0417164300")).thenReturn(true);
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban("NL91ABNA0417164301")).thenReturn(true);
                
                when(accountRepository.findByIban("NL91ABNA0417164300")).thenReturn(Optional.of(senderAccount));
                when(accountRepository.findByIban("NL91ABNA0417164301")).thenReturn(Optional.of(receiverAccount));
                when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
                when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
                when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
                when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

                // When
                assertDoesNotThrow(() -> {
                    transactionService.transferMoneyByIban("NL91ABNA0417164300", "NL91ABNA0417164301", 
                        new BigDecimal("100.00"), "IBAN transfer test");
                });

                // Then
                verify(accountRepository, times(2)).save(any(Account.class));
                verify(transactionRepository, times(1)).save(any(Transaction.class));
            }
        }

        @Test
        @DisplayName("Should throw exception when sender IBAN is invalid")
        void shouldThrowExceptionWhenSenderIbanInvalid() {
            // Given
            try (var mockedIbanGenerator = mockStatic(IbanGenerator.class)) {
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban("INVALID_IBAN")).thenReturn(false);

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    transactionService.transferMoneyByIban("INVALID_IBAN", "NL91ABNA0417164301", 
                        new BigDecimal("100.00"), "Test transfer");
                });

                assertEquals("Invalid sender IBAN", exception.getMessage());
                verify(accountRepository, never()).findByIban(any());
            }
        }

        @Test
        @DisplayName("Should throw exception when receiver IBAN is invalid")
        void shouldThrowExceptionWhenReceiverIbanInvalid() {
            // Given
            try (var mockedIbanGenerator = mockStatic(IbanGenerator.class)) {
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban("NL91ABNA0417164300")).thenReturn(true);
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban("INVALID_IBAN")).thenReturn(false);

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    transactionService.transferMoneyByIban("NL91ABNA0417164300", "INVALID_IBAN", 
                        new BigDecimal("100.00"), "Test transfer");
                });

                assertEquals("Invalid receiver IBAN", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should throw exception when sender account not found by IBAN")
        void shouldThrowExceptionWhenSenderAccountNotFoundByIban() {
            // Given
            try (var mockedIbanGenerator = mockStatic(IbanGenerator.class)) {
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban(any())).thenReturn(true);
                when(accountRepository.findByIban("NL91ABNA0417164300")).thenReturn(Optional.empty());

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    transactionService.transferMoneyByIban("NL91ABNA0417164300", "NL91ABNA0417164301", 
                        new BigDecimal("100.00"), "Test transfer");
                });

                assertEquals("Sender account not found", exception.getMessage());
            }
        }

        @Test
        @DisplayName("Should throw exception when receiver account not found by IBAN")
        void shouldThrowExceptionWhenReceiverAccountNotFoundByIban() {
            // Given
            try (var mockedIbanGenerator = mockStatic(IbanGenerator.class)) {
                mockedIbanGenerator.when(() -> IbanGenerator.validateIban(any())).thenReturn(true);
                when(accountRepository.findByIban("NL91ABNA0417164300")).thenReturn(Optional.of(senderAccount));
                when(accountRepository.findByIban("NL91ABNA0417164301")).thenReturn(Optional.empty());

                // When & Then
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                    transactionService.transferMoneyByIban("NL91ABNA0417164300", "NL91ABNA0417164301", 
                        new BigDecimal("100.00"), "Test transfer");
                });

                assertEquals("Receiver account not found", exception.getMessage());
            }
        }
    }

    @Nested
    @DisplayName("Transaction History Tests")
    class TransactionHistoryTests {

        @Test
        @DisplayName("Should get user transaction history with both transactions and ATM operations")
        void shouldGetUserTransactionHistoryWithBothTypes() {
            // Given
            List<Transaction> transactions = List.of(transaction);
            List<AtmOperation> atmOperations = List.of(atmOperation);

            when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
                .thenReturn(transactions);
            when(atmOperationRepository.findByAccount_User_Id(1L))
                .thenReturn(atmOperations);

            // When
            List<TransactionHistoryDTO> result = transactionService.getUserTransactionHistory(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(transactionRepository).findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L);
            verify(atmOperationRepository).findByAccount_User_Id(1L);
        }

        @Test
        @DisplayName("Should get empty transaction history for user with no transactions")
        void shouldGetEmptyTransactionHistoryForUserWithNoTransactions() {
            // Given
            when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
                .thenReturn(new ArrayList<>());
            when(atmOperationRepository.findByAccount_User_Id(1L))
                .thenReturn(new ArrayList<>());

            // When
            List<TransactionHistoryDTO> result = transactionService.getUserTransactionHistory(1L);

            // Then
            assertNotNull(result);
            assertEquals(0, result.size());
        }

        @Test
        @DisplayName("Should get account transaction history by account ID")
        void shouldGetAccountTransactionHistoryById() {
            // Given
            List<Transaction> transactions = List.of(transaction);
            List<AtmOperation> atmOperations = List.of(atmOperation);

            when(transactionRepository.findByFromAccount_IdOrToAccount_Id(1L, 1L))
                .thenReturn(transactions);
            when(atmOperationRepository.findByAccount_Id(1L))
                .thenReturn(atmOperations);

            // When
            List<TransactionHistoryDTO> result = transactionService.getAccountTransactionHistory(1L);

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(transactionRepository).findByFromAccount_IdOrToAccount_Id(1L, 1L);
            verify(atmOperationRepository).findByAccount_Id(1L);
        }

        @Test
        @DisplayName("Should get account transaction history by IBAN")
        void shouldGetAccountTransactionHistoryByIban() {
            // Given
            List<Transaction> transactions = List.of(transaction);
            List<AtmOperation> atmOperations = List.of(atmOperation);

            when(accountRepository.findByIban("NL91ABNA0417164300")).thenReturn(Optional.of(senderAccount));
            when(transactionRepository.findByFromAccount_IdOrToAccount_Id(1L, 1L))
                .thenReturn(transactions);
            when(atmOperationRepository.findByAccount_Id(1L))
                .thenReturn(atmOperations);

            // When
            List<TransactionHistoryDTO> result = transactionService.getAccountTransactionHistoryByIban("NL91ABNA0417164300");

            // Then
            assertNotNull(result);
            assertEquals(2, result.size());
            verify(accountRepository).findByIban("NL91ABNA0417164300");
        }

        @Test
        @DisplayName("Should throw exception when account not found by IBAN for history")
        void shouldThrowExceptionWhenAccountNotFoundByIbanForHistory() {
            // Given
            when(accountRepository.findByIban("INVALID_IBAN")).thenReturn(Optional.empty());

            // When & Then
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                transactionService.getAccountTransactionHistoryByIban("INVALID_IBAN");
            });

            assertEquals("Account not found for IBAN: INVALID_IBAN", exception.getMessage());
        }

        @Test
        @DisplayName("Should get legacy account history")
        void shouldGetLegacyAccountHistory() {
            // Given
            List<Transaction> transactions = List.of(transaction);
            when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L))
                .thenReturn(transactions);

            // When
            List<Transaction> result = transactionService.getAccountHistory(1L);

            // Then
            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(transaction.getId(), result.get(0).getId());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Boundary Tests")
    class EdgeCasesAndBoundaryTests {

        @Test
        @DisplayName("Should handle very large transfer amounts")
        void shouldHandleVeryLargeTransferAmounts() {
            // Given
            BigDecimal largeAmount = new BigDecimal("999999999.99");
            senderAccount.setBalance(new BigDecimal("1000000000.00"));
            
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When & Then
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, largeAmount, "Large transfer");
            });
        }

        @Test
        @DisplayName("Should handle very small transfer amounts")
        void shouldHandleVerySmallTransferAmounts() {
            // Given
            BigDecimal smallAmount = new BigDecimal("0.01");
            
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When & Then
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, smallAmount, "Small transfer");
            });
        }

        @Test
        @DisplayName("Should handle null description in transfer")
        void shouldHandleNullDescriptionInTransfer() {
            // Given
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When & Then
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), null);
            });
        }

        @Test
        @DisplayName("Should handle empty description in transfer")
        void shouldHandleEmptyDescriptionInTransfer() {
            // Given
            when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
            when(accountRepository.findById(2L)).thenReturn(Optional.of(receiverAccount));
            when(accountRepository.save(any(Account.class))).thenReturn(senderAccount);
            when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);

            // When & Then
            assertDoesNotThrow(() -> {
                transactionService.transferMoney(1L, 2L, new BigDecimal("100.00"), "");
            });
        }
    }
}