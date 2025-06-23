package com.bankapp.repository;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    List<Account> findByApprovedFalse();
    List<Account> findByApprovedTrue();
    List<Account> findByApprovedTrueAndClosedFalse();
    Optional<Account> findByIban(String iban);
    
    @Query("""
        SELECT a.user FROM Account a
        WHERE a.iban = :iban
        AND a.approved = true
        AND a.closed = false
        AND a.user.registrationStatus = com.bankapp.model.enums.RegistrationStatus.APPROVED
        AND LOWER(a.user.role) = LOWER('CUSTOMER')
        """)
    List<User> findActiveCustomersByIban(@Param("iban") String iban);
}
