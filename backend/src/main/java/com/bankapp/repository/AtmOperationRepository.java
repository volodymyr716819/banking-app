package com.bankapp.repository;

import java.util.List;

import com.bankapp.model.AtmOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtmOperationRepository extends JpaRepository<AtmOperation, Long> {
    List<AtmOperation> findByAccount_User_Id(Long userId);
    List<AtmOperation> findByAccount_Id(Long accountId);
}