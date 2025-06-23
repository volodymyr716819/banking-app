/*package com.bankapp.service;

import com.bankapp.model.Account;
import com.bankapp.model.Transaction;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TransferLogicTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        User sender = new User();
        sender.setId(1L);
        sender.setName("John Smith");
        sender.setApproved(true);

        User receiver = new User();
        receiver.setId(2L);
        receiver.setName("Jane Doe");
        receiver.setApproved(true);

        senderAccount = new Account();
        senderAccount.setIban("DE1111111111");
        senderAccount.setUser(sender);
        senderAccount.setType("CHECKING");
        senderAccount.setBalance(new BigDecimal("1000.00"));
        senderAccount.setApproved(true);
        senderAccount.setClosed(false);

        receiverAccount = new Account();
        receiverAccount.setIban("DE2222222222");
        receiverAccount.setUser(receiver);
        receiverAccount.setType("SAVINGS");
        receiverAccount.setBalance(new BigDecimal("500.00"));
        receiverAccount.setApproved(true);
        receiverAccount.setClosed(false);
    }

    @Test
    @DisplayName("Should transfer money successfully")
    void shouldTransferMoneySuccessfully() {
        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(i -> i.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArgument(0));

        transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("200.00"), "Test transfer");

        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());
        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Should throw exception when transfer amount is zero or negative")
    void shouldThrowExceptionWhenTransferAmountIsZeroOrNegative() {
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney("DE1111111111", "DE2222222222", BigDecimal.ZERO, "Test");
        });
        assertEquals("Amount must be greater than zero", ex1.getMessage());

        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("-100"), "Test");
        });
        assertEquals("Amount must be greater than zero", ex2.getMessage());

        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when sender has insufficient balance")
    void shouldThrowExceptionWhenSenderHasInsufficientBalance() {
        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("1500.00"), "Overspend");
        });

        assertEquals("Insufficient balance", ex.getMessage());
        verify(accountRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }
}
*/