package com.bankapp.service;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication mockAuthentication;
    
    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDeclineUser_Success() {
        // Setup: user pending approval
        User user = new User();
        user.setRegistrationStatus(RegistrationStatus.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Action
        userService.declineUser(1L);

        // Verify status changed to DECLINED
        assertEquals(RegistrationStatus.DECLINED, user.getRegistrationStatus());
    }

    @Test
    void testDeclineUser_NotPending_Throws() {
        // Setup: user already approved
        User user = new User();
        user.setRegistrationStatus(RegistrationStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Verify: throws if not pending
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.declineUser(1L));
        assertEquals("User is not pending", ex.getMessage());
    }

    @Test
    void testValidateLogin_Success() {
        // Setup login request + stored user match
        User request = new User();
        request.setEmail("user@test.com");
        request.setPassword("pass123");

        User stored = new User();
        stored.setEmail("user@test.com");
        stored.setPassword("hashedPass");
        stored.setApproved(true);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("pass123", "hashedPass")).thenReturn(true);

        // Act & Assert
        Optional<User> result = userService.validateLogin(request);
        assertTrue(result.isPresent());
    }

    @Test
    void testValidateLogin_FailOnPasswordMismatch() {
        // Setup: password mismatch
        User request = new User();
        request.setEmail("user@test.com");
        request.setPassword("wrong");

        User stored = new User();
        stored.setEmail("user@test.com");
        stored.setPassword("hashedPass");
        stored.setApproved(true);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(stored));
        when(passwordEncoder.matches("wrong", "hashedPass")).thenReturn(false);

        // Expect empty on password mismatch
        assertTrue(userService.validateLogin(request).isEmpty());
    }

    @Test
    void testValidateLogin_UserNotFound() {
        // No user in DB
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        User req = new User();
        req.setEmail("missing@test.com");
        req.setPassword("pass");

        // Expect empty
        assertTrue(userService.validateLogin(req).isEmpty());
    }

    @Test
    void testRegisterUser_Success() {
        // Setup new user with no existing email
        User user = new User();
        user.setEmail("new@test.com");
        user.setPassword("plain");

        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("plain")).thenReturn("hashed");

        User savedUser = new User();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        // Act
        User result = userService.registerUser(user);

        // Verify encoded pass, role, and pending status
        assertEquals("hashed", user.getPassword());
        assertEquals("customer", user.getRole());
        assertEquals(RegistrationStatus.PENDING, user.getRegistrationStatus());
        assertEquals(savedUser, result);
    }

    @Test
    void testRegisterUser_EmailAlreadyExists() {
        // Email already registered
        User user = new User();
        user.setEmail("exist@test.com");

        when(userRepository.findByEmail("exist@test.com")).thenReturn(Optional.of(new User()));

        // Expect rejection
        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.registerUser(user));
        assertEquals("Email is already registered.", ex.getMessage());
    }

    @Test
    void testValidateAuthentication_Success() {
        // Setup an approved user
        User user = new User();
        user.setEmail("approved@example.com");
        user.setApproved(true);

        // Mock authentication and repository
        when(mockAuthentication.getName()).thenReturn("approved@example.com");
        when(mockAuthentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByEmail("approved@example.com")).thenReturn(Optional.of(user));

       // Execute and verify
       Optional<User> result = userService.validateAuthentication(mockAuthentication);
       assertTrue(result.isPresent(), "Expected user to be present for approved authenticated user");
    }


    @Test
    void testValidateAuthentication_AccountNotApproved() {
        // Setup a user who is not approved
        User user = new User();
        user.setEmail("notapproved@example.com");
        user.setApproved(false);

       // Mock authentication and repository
       when(mockAuthentication.getName()).thenReturn("notapproved@example.com");
       when(mockAuthentication.isAuthenticated()).thenReturn(true);
       when(userRepository.findByEmail("notapproved@example.com")).thenReturn(Optional.of(user));

        // Expect exception when account is not approved
       assertThrows(RuntimeException.class, () -> {
       userService.validateAuthentication(mockAuthentication);
       }, "Expected RuntimeException for unapproved user");
    }

    @Test
    void testApproveUser_Success() {
        // Setup user pending approval
        User user = new User();
        user.setRegistrationStatus(RegistrationStatus.PENDING);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // Act
        userService.approveUser(10L);

        // Assert approval status
        assertEquals(RegistrationStatus.APPROVED, user.getRegistrationStatus());
    }

    @Test
    void testApproveUser_NotPending_Throws() {
        // Setup: already declined user
        User user = new User();
        user.setRegistrationStatus(RegistrationStatus.DECLINED);

        when(userRepository.findById(10L)).thenReturn(Optional.of(user));

        // Expect exception if not pending
        assertThrows(RuntimeException.class, () -> userService.approveUser(10L));
    }
}
