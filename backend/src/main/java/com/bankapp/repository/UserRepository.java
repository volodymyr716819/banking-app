package com.bankapp.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.bankapp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Custom query to find a user by email (used in login)
    Optional<User> findByEmail(String email);
}
