package com.bankapp.service;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_Success() {
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password123");
        user.setBsn("123456789");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123"))
                .thenReturn("hashedPassword");
        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        User result = userService.registerUser(user);

        assertEquals("hashedPassword", result.getPassword());
        assertEquals(RegistrationStatus.PENDING, result.getRegistrationStatus());
        verify(userRepository).save(user);
    }

    @Test
    void approveUser_Success() {
        User user = new User();
        user.setRegistrationStatus(RegistrationStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.approveUser(1L);

        assertEquals(RegistrationStatus.APPROVED, user.getRegistrationStatus());
    }

    @Test
    void validateLogin_Success() {
        User loginRequest = new User();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        User storedUser = new User();
        storedUser.setEmail("test@example.com");
        storedUser.setPassword("hashedPassword");
        storedUser.setApproved(true);

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(storedUser));
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);

        Optional<User> result = userService.validateLogin(loginRequest);

        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
    }
}