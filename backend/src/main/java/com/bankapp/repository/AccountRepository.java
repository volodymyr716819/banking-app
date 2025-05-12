package com.bankapp.repository;

import com.bankapp.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByApprovedFalse();
}
