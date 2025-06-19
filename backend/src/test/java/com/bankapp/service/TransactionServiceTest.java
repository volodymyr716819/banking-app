package com.bankapp.service;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.model.*;
import com.bankapp.model.Transaction.TransactionType;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.TransactionRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AtmOperationRepository atmOperationRepository;

    @InjectMocks
    private TransactionService transactionService;

    private Account senderAccount;
    private Account receiverAccount;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        transactionService = new TransactionService(transactionRepository, accountRepository, atmOperationRepository);

        senderAccount = new Account();
        senderAccount.setId(1L);
        senderAccount.setIban("DE1111111111");
        senderAccount.setApproved(true);
        senderAccount.setClosed(false);
        senderAccount.setBalance(new BigDecimal("1000.00"));

        receiverAccount = new Account();
        receiverAccount.setId(2L);
        receiverAccount.setIban("DE2222222222");
        receiverAccount.setApproved(true);
        receiverAccount.setClosed(false);
        receiverAccount.setBalance(new BigDecimal("500.00"));
    }

    @Test
    void testTransferMoney_Success() {
        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("200.00"), "Test transfer");

        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());

        verify(accountRepository, times(2)).save(any(Account.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void testTransferMoney_InsufficientBalance() {
        senderAccount.setBalance(new BigDecimal("100.00"));

        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        assertThrows(IllegalArgumentException.class, () -> 
            transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("200.00"), "Overdraft")
        );
    }

    @Test
    void testTransferMoney_UnapprovedAccount() {
        senderAccount.setApproved(false);

        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        assertThrows(IllegalArgumentException.class, () -> 
            transactionService.transferMoney("DE1111111111", "DE2222222222", new BigDecimal("50.00"), "Invalid")
        );
    }

    @Test
    void testDepositMoney_Success() {
        senderAccount.setDailyLimit(new BigDecimal("2000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(transactionRepository.sumDepositsForToday(1L)).thenReturn(BigDecimal.ZERO);

        transactionService.depositMoney(1L, new BigDecimal("500.00"));

        assertEquals(new BigDecimal("1500.00"), senderAccount.getBalance());
        verify(accountRepository).save(senderAccount);
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testDepositMoney_ExceedsLimit() {
        senderAccount.setDailyLimit(new BigDecimal("500.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(senderAccount));
        when(transactionRepository.sumDepositsForToday(1L)).thenReturn(new BigDecimal("400.00"));

        assertThrows(IllegalArgumentException.class, () ->
            transactionService.depositMoney(1L, new BigDecimal("200.00"))
        );
    }

    @Test
    void testGetTransactionHistoryByIbanWithAuth_EmployeeAccess() {
        User employee = new User();
        employee.setId(999L);
        employee.setRole("EMPLOYEE");

        Account acc = new Account();
        acc.setIban("IBAN1");
        acc.setUser(new User());
        acc.getUser().setId(1L);

        when(accountRepository.findByIban("IBAN1")).thenReturn(Optional.of(acc));
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L)).thenReturn(Collections.emptyList());
        when(atmOperationRepository.findByAccount_User_Id(1L)).thenReturn(Collections.emptyList());

        List<TransactionHistoryDTO> result = transactionService.getTransactionHistoryByIbanWithAuth(
                "IBAN1", employee, null, null, null, null);

        assertNotNull(result);
    }
}
