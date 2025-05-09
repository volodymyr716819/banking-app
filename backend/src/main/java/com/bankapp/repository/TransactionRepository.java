package com.bankapp.repository;

import com.bankapp.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findBySenderAccountIdOrReceiverAccountId(Long senderAccountId, Long receiverAccountId);
    List<Transaction> findByAccountIn(List<Account> accounts);
}
