package com.bankapp.repository;

import java.util.List;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import com.bankapp.model.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByFromAccount_User_IdOrToAccount_User_Id(Long fromUserId, Long toUserId);
    
    List<Transaction> findByFromAccount_IdOrToAccount_Id(Long fromAccountId, Long toAccountId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
       "WHERE t.toAccount.id = :accountId " +
       "AND t.transactionType = com.bankapp.model.Transaction$TransactionType.DEPOSIT " +
       "AND FUNCTION('DATE', t.timestamp) = CURRENT_DATE")
    BigDecimal sumDepositsForToday(@Param("accountId") Long accountId);
}
