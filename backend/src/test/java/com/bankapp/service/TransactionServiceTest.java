package com.bankapp.service;

import com.bankapp.dto.TransactionHistoryDTO;
import com.bankapp.dto.TransferRequest;
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
    void testProcessTransfer_Success() {
        TransferRequest request = new TransferRequest();
        request.setSenderIban("DE1111111111");
        request.setReceiverIban("DE2222222222");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Test transfer");

        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        transactionService.processTransfer(request);

        assertEquals(new BigDecimal("800.00"), senderAccount.getBalance());
        assertEquals(new BigDecimal("700.00"), receiverAccount.getBalance());

        verify(accountRepository).saveAll(anyList());
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void testProcessTransfer_InsufficientBalance() {
        senderAccount.setBalance(new BigDecimal("100.00"));

        TransferRequest request = new TransferRequest();
        request.setSenderIban("DE1111111111");
        request.setReceiverIban("DE2222222222");
        request.setAmount(new BigDecimal("200.00"));
        request.setDescription("Failing transfer");

        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void testProcessTransfer_UnapprovedAccount() {
        senderAccount.setApproved(false);

        TransferRequest request = new TransferRequest();
        request.setSenderIban("DE1111111111");
        request.setReceiverIban("DE2222222222");
        request.setAmount(new BigDecimal("100.00"));
        request.setDescription("Unapproved");

        when(accountRepository.findByIban("DE1111111111")).thenReturn(Optional.of(senderAccount));
        when(accountRepository.findByIban("DE2222222222")).thenReturn(Optional.of(receiverAccount));

        assertThrows(IllegalArgumentException.class, () -> transactionService.processTransfer(request));
    }

    @Test
    void testGetTransactionHistoryByIbanWithAuth_EmployeeAccess() {
        User employee = new User();
        employee.setId(999L);
        employee.setRole("EMPLOYEE");

        Account acc = new Account();
        acc.setIban("IBAN1");
        User user = new User();
        user.setId(1L);
        acc.setUser(user);

        when(accountRepository.findByIban("IBAN1")).thenReturn(Optional.of(acc));
        when(transactionRepository.findByFromAccount_User_IdOrToAccount_User_Id(1L, 1L)).thenReturn(Collections.emptyList());
        when(atmOperationRepository.findByAccount_User_Id(1L)).thenReturn(Collections.emptyList());

        List<TransactionHistoryDTO> result = transactionService.getTransactionHistoryByIbanWithAuth(
            "IBAN1", employee, null, null, null, null);

        assertNotNull(result);
    }
}