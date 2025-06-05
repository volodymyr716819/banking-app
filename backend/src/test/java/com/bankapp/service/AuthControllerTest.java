package com.bankapp.controller;

import com.bankapp.model.User;
import com.bankapp.security.JwtUtil;
import com.bankapp.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    // Mock dependencies
    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private UserService userService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private Authentication authentication;

    @Mock
    private UserDetails userDetails;

    // Inject mocks into AuthController
    @InjectMocks
    private AuthController authController;

    // Initialize mocks before each test
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testLoginSuccess() {
        // Arrange: set up a valid user object with email and password
        User user = new User();
        user.setEmail("test@example.com");
        user.setPassword("password");

        // Mock successful authentication and token generation
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(userService.validateLogin(user)).thenReturn(Optional.of(user));
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("mock-jwt-token");

        // Act: call the login method
        ResponseEntity<?> response = authController.login(user);

        // Assert: verify that status is OK and token is returned
        assertEquals(200, response.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertEquals("mock-jwt-token", body.get("token"));
    }

    @Test
    void testLoginFailure_InvalidCredentials() {
        // Arrange: simulate authentication failure
        User user = new User();
        user.setEmail("wrong@example.com");
        user.setPassword("wrongpass");

        // Mock exception thrown when credentials are invalid
        when(authenticationManager.authenticate(any())).thenThrow(new RuntimeException("Bad credentials"));

        // Act: call the login method
        ResponseEntity<?> response = authController.login(user);

        // Assert: expect 401 Unauthorized status
        assertEquals(401, response.getStatusCodeValue());
    }

    @Test
    void testRegisterSuccess() {
        // Arrange: simulate successful registration
        User user = new User();
        when(userService.registerUser(user)).thenReturn(user);

        // Act: call the register method
        ResponseEntity<?> response = authController.register(user);

        // Assert: expect 200 OK status
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testRegisterFailure_DuplicateEmail() {
        // Arrange: simulate registration failure due to duplicate email
        User user = new User();
        when(userService.registerUser(user)).thenThrow(new RuntimeException("Email is already registered."));

        // Act: call the register method
        ResponseEntity<?> response = authController.register(user);

        // Assert: expect 400 Bad Request status
        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void testValidateTokenSuccess() {
        // Arrange: simulate valid authentication and user session
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setName("John");
        user.setRole("customer");

        // Mock successful user validation
        when(userService.validateAuthentication(authentication)).thenReturn(Optional.of(user));

        // Act: call the validateToken method
        ResponseEntity<?> response = authController.validateToken(authentication);

        // Assert: expect 200 OK and valid = true in the response
        assertEquals(200, response.getStatusCodeValue());
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertTrue((Boolean) body.get("valid"));
        assertEquals("test@example.com", body.get("email"));
    }

    @Test
    void testValidateTokenFailure() {
        // Arrange: simulate failure to authenticate user
        when(userService.validateAuthentication(authentication)).thenReturn(Optional.empty());

        // Act: call the validateToken method
        ResponseEntity<?> response = authController.validateToken(authentication);

        // Assert: expect 401 Unauthorized status
        assertEquals(401, response.getStatusCodeValue());
    }
}
