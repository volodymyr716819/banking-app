package com.bankapp.service;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public void declineUser(Long id) {
        User user = repo.findById(id)
                        .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRegistrationStatus() != RegistrationStatus.PENDING) {
            throw new RuntimeException("User is not pending");
        }

        user.setRegistrationStatus(RegistrationStatus.DECLINED);
    }
}