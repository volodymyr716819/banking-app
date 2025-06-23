package com.bankapp.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bankapp.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccount_User_IdOrToAccount_User_Id(Long fromUserId, Long toUserId);
    
    List<Transaction> findByFromAccount_IdOrToAccount_Id(Long fromAccountId, Long toAccountId);
}
// test for merge