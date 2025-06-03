package com.bankapp.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.bankapp.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    List<User> findByApprovedFalse();

    List<User> findByRoleIgnoreCase(String role);

    List<User> findByApprovedTrue();
    

    // Unified search method for a single term
    @Query("SELECT u FROM User u WHERE " +
           "(LOWER(u.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :term, '%'))) AND " +
           "u.approved = true AND LOWER(u.role) = LOWER('customer')")
    List<User> findCustomersBySearchTerm(@Param("term") String term);
    
    // Find users by a combination of name and/or email
    @Query("SELECT u FROM User u WHERE " +
           "(:name IS NULL OR LOWER(u.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:email IS NULL OR LOWER(u.email) LIKE LOWER(CONCAT('%', :email, '%'))) AND " +
           "u.approved = true AND LOWER(u.role) = LOWER(:role)")
    List<User> findByNameAndEmailAndRole(
            @Param("name") String name,
            @Param("email") String email,
            @Param("role") String role);
}