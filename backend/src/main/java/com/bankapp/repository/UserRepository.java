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

    // Finds a user by their email address
    Optional<User> findByEmail(String email);

    // Status-based lookup methods
    List<User> findByRegistrationStatus(RegistrationStatus registrationStatus);

    List<User> findByRegistrationStatusAndRoleIgnoreCase(RegistrationStatus registrationStatus,
                                                         String role);

    List<User> findByNameContainingIgnoreCaseAndRegistrationStatusAndRoleIgnoreCase(
            String name,
            RegistrationStatus registrationStatus,
            String role);

    List<User> findByEmailContainingIgnoreCaseAndRegistrationStatusAndRoleIgnoreCase(
            String email,
            RegistrationStatus registrationStatus,
            String role);

    // Search methods for finding approved customers
    @Query("""
           SELECT u FROM User u
           WHERE (LOWER(u.name)  LIKE LOWER(CONCAT('%', :term, '%'))
               OR LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%')))
             AND u.registrationStatus = com.bankapp.model.enums.RegistrationStatus.APPROVED
             AND LOWER(u.role) = LOWER('customer')
           """)
    List<User> findApprovedCustomersBySearchTerm(@Param("term") String term);

    @Query("""
           SELECT u FROM User u
           WHERE (:name  IS NULL OR LOWER(u.name)  LIKE LOWER(CONCAT('%', :name,  '%')))
             AND (:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%')))
             AND u.registrationStatus = com.bankapp.model.enums.RegistrationStatus.APPROVED
             AND LOWER(u.role) = LOWER(:role)
           """)
    List<User> searchApprovedByNameEmailAndRole(@Param("name")  String name,
                                                @Param("email") String email,
                                                @Param("role")  String role);
}