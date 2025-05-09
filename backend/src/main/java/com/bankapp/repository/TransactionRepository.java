package com.bankapp.repository;

import com.bankapp.model.Transaction;
import com.bankapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; 

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByAccountIn(List<Account> accounts);
}
