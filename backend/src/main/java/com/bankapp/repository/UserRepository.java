package com.bankapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bankapp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByApprovedFalse();

    List<User> findByRoleIgnoreCase(String role);

    List<User> findByApprovedTrue();
    
    List<User> findByNameContainingIgnoreCaseAndApprovedTrueAndRoleIgnoreCase(String name, String role);
}
