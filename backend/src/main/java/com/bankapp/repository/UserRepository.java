package com.bankapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /* user lookup */
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.deleted = false")
    Optional<User> findByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.bsn = :bsn AND u.deleted = false")
    Optional<User> findByBsn(@Param("bsn") String bsn);
  
    @Query("SELECT u FROM User u WHERE u.deleted = false AND LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.registrationStatus = :status")
    List<User> findByRegistrationStatus(@Param("status") RegistrationStatus registrationStatus);
    
    @Query("SELECT u FROM User u WHERE u.deleted = false AND u.registrationStatus = :status AND LOWER(u.role) = LOWER(:role)")
    List<User> findByRegistrationStatusAndRoleIgnoreCase(@Param("status") RegistrationStatus status, @Param("role") String role);
}