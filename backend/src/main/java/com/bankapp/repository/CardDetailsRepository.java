package com.bankapp.repository;

import com.bankapp.model.CardDetails;
import com.bankapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface CardDetailsRepository extends JpaRepository<CardDetails, Long> {
    Optional<CardDetails> findByAccount(Account account);
    Optional<CardDetails> findByAccountId(Long accountId);
    
    @Modifying
    @Transactional
    @Query("DELETE FROM CardDetails c WHERE c.account.id = :accountId")
    void deleteByAccountId(Long accountId);
    
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM card_details WHERE account_id = :accountId", nativeQuery = true)
    void deleteByAccountIdNative(Long accountId);
}