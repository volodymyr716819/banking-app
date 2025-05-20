package com.bankapp.repository;

import com.bankapp.model.CardDetails;
import com.bankapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardDetailsRepository extends JpaRepository<CardDetails, Long> {
    Optional<CardDetails> findByAccount(Account account);
    Optional<CardDetails> findByAccountId(Long accountId);
}