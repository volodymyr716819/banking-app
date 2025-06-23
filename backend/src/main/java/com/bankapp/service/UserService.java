package com.bankapp.service;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import com.bankapp.exception.UnapprovedAccountException;

import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class UserService {

    private final UserRepository repo;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository repo, PasswordEncoder passwordEncoder) {
        this.repo = repo;
        this.passwordEncoder = passwordEncoder;
    }

    // Used to decline users pending approval
    public void declineUser(Long id) {
        User user = repo.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("User is not pending");
        }

        user.setRegistrationStatus(RegistrationStatus.DECLINED);
    }

     // Validates login input
    public Optional<User> validateLogin(User request) {
        Optional<User> found = repo.findByEmail(request.getEmail());

        if (found.isEmpty()) {
            return Optional.empty();
        }

        User user = found.get();
        
        // Allow login regardless of approval status
        // The frontend will handle redirecting unapproved users to the waiting page
        
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return Optional.empty();
        }

        return Optional.of(user);
    }

    // Handles registration logic and validation
    public User registerUser(User user) {
        if (repo.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email is already registered.");
        }
        
        // Validate BSN
        if (user.getBsn() == null || user.getBsn().trim().isEmpty()) {
            throw new RuntimeException("BSN is required.");
        }
        
        // Check if BSN is already registered
        if (repo.findByBsn(user.getBsn()).isPresent()) {
            throw new RuntimeException("BSN is already registered.");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("customer");
        user.setRegistrationStatus(RegistrationStatus.PENDING);
        return repo.save(user);
    }

    // Validates authentication object
    public Optional<User> validateAuthentication(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            Optional<User> userOpt = repo.findByEmail(authentication.getName());

            if (userOpt.isEmpty()) return Optional.empty();

            User user = userOpt.get();
            // We don't throw an exception for unapproved accounts anymore
            // The frontend will handle limiting access based on approval status
            
            return Optional.of(user);
        }

        return Optional.empty();
    }

    public void approveUser(Long id) {
        User user = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("User is not pending");
        }
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
    }
}