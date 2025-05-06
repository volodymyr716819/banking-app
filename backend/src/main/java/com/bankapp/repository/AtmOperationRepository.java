package com.bankapp.repository;

import com.bankapp.model.AtmOperation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtmOperationRepository extends JpaRepository<AtmOperation, Long> {
}