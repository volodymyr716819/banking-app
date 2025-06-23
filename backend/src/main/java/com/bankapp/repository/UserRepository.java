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
    Optional<User> findByEmail(String email);
    
    Optional<User> findByBsn(String bsn);
    
    @Query("SELECT u FROM User u WHERE LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<User> findByNameContainingIgnoreCase(@Param("name") String name);
    
    List<User> findByRegistrationStatus(RegistrationStatus registrationStatus);
    List<User> findByRegistrationStatusAndRoleIgnoreCase(RegistrationStatus registrationStatus, String role);
}