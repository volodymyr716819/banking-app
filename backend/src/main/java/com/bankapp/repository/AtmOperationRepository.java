package com.bankapp.repository;

import com.bankapp.model.AtmOperation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtmOperationRepository extends JpaRepository<AtmOperation, Long> {
    
    /**
     * Find ATM operations by account ID
     * 
     * @param accountId The account ID
     * @return List of ATM operations
     */
    List<AtmOperation> findByAccountId(Long accountId);
    
    /**
     * Find all ATM operations for a user's accounts
     * 
     * @param userId User ID
     * @return List of ATM operations
     */
    @Query("SELECT a FROM AtmOperation a WHERE a.account.user.id = :userId")
    List<AtmOperation> findByUserId(@Param("userId") Long userId);
    
    /**
     * Find all ATM operations for a user's accounts of a specific type
     * 
     * @param userId User ID
     * @param accountType Account type (CHECKING or SAVINGS)
     * @return List of ATM operations
     */
    @Query("SELECT a FROM AtmOperation a WHERE a.account.user.id = :userId AND a.account.type = :accountType")
    List<AtmOperation> findByUserIdAndAccountType(@Param("userId") Long userId, @Param("accountType") String accountType);
}