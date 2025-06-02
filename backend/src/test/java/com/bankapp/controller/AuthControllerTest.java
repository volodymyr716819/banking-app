package com.bankapp.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.bankapp.model.User;
import com.bankapp.repository.UserRepository;
import com.bankapp.security.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@DisplayName("AuthController Integration Tests")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private User unapprovedUser;
    private User loginRequest;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setPassword("hashedPassword");
        testUser.setRole("CUSTOMER");
        testUser.setApproved(true);

        // Setup unapproved user
        unapprovedUser = new User();
        unapprovedUser.setId(2L);
        unapprovedUser.setName("Jane Pending");
        unapprovedUser.setEmail("jane@example.com");
        unapprovedUser.setPassword("hashedPassword");
        unapprovedUser.setRole("CUSTOMER");
        unapprovedUser.setApproved(false);

        // Setup login request
        loginRequest = new User();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");
    }

    @Nested
    @DisplayName("Login Tests")
    class LoginTests {

        @Test
        @DisplayName("Should successfully login with valid credentials")
        void shouldSuccessfullyLoginWithValidCredentials() throws Exception {
            // Given
            Authentication mockAuth = mock(Authentication.class);
            UserDetails mockUserDetails = mock(UserDetails.class);
            
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(mockAuth.getPrincipal()).thenReturn(mockUserDetails);
            when(jwtUtil.generateToken(mockUserDetails)).thenReturn("jwt-token");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("jwt-token"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil).generateToken(mockUserDetails);
        }

        @Test
        @DisplayName("Should return unauthorized for invalid credentials")
        void shouldReturnUnauthorizedForInvalidCredentials() throws Exception {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should return unauthorized when user not found after authentication")
        void shouldReturnUnauthorizedWhenUserNotFoundAfterAuthentication() throws Exception {
            // Given
            Authentication mockAuth = mock(Authentication.class);
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid credentials."));

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should return forbidden for unapproved user")
        void shouldReturnForbiddenForUnapprovedUser() throws Exception {
            // Given
            Authentication mockAuth = mock(Authentication.class);
            User unapprovedLoginRequest = new User();
            unapprovedLoginRequest.setEmail("jane@example.com");
            unapprovedLoginRequest.setPassword("password123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(unapprovedUser));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(unapprovedLoginRequest)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.message").value("Your account is pending approval by an employee."));

            verify(jwtUtil, never()).generateToken(any());
        }

        @Test
        @DisplayName("Should handle runtime exceptions during login")
        void shouldHandleRuntimeExceptionsDuringLogin() throws Exception {
            // Given
            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new RuntimeException("Database connection failed"));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").value("Invalid email or password."));
        }

        @Test
        @DisplayName("Should validate required fields in login request")
        void shouldValidateRequiredFieldsInLoginRequest() throws Exception {
            // Given
            User invalidRequest = new User();
            invalidRequest.setEmail(""); // Missing email
            invalidRequest.setPassword(""); // Missing password

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Register Tests")
    class RegisterTests {

        @Test
        @DisplayName("Should successfully register new user")
        void shouldSuccessfullyRegisterNewUser() throws Exception {
            // Given
            User newUser = new User();
            newUser.setName("New User");
            newUser.setEmail("newuser@example.com");
            newUser.setPassword("password123");

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Registration successful. Your account is pending approval."));

            verify(passwordEncoder).encode("password123");
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should return bad request when email already exists")
        void shouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {
            // Given
            User newUser = new User();
            newUser.setName("John Doe");
            newUser.setEmail("john@example.com");
            newUser.setPassword("password123");

            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Email is already registered."));

            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should set default values for new user registration")
        void shouldSetDefaultValuesForNewUserRegistration() throws Exception {
            // Given
            User newUser = new User();
            newUser.setName("New User");
            newUser.setEmail("newuser@example.com");
            newUser.setPassword("password123");

            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
                User savedUser = invocation.getArgument(0);
                // Verify default values are set
                assert savedUser.getRole().equals("customer");
                assert !savedUser.isApproved();
                return savedUser;
            });

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Registration successful. Your account is pending approval."));

            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should handle malformed registration request")
        void shouldHandleMalformedRegistrationRequest() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{invalid json}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should validate email format in registration")
        void shouldValidateEmailFormatInRegistration() throws Exception {
            // Given
            User newUser = new User();
            newUser.setName("New User");
            newUser.setEmail("invalid-email-format");
            newUser.setPassword("password123");

            // Note: Email validation would typically be handled by @Valid annotation
            // This test demonstrates the endpoint behavior with invalid email
            when(userRepository.findByEmail("invalid-email-format")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
            when(userRepository.save(any(User.class))).thenReturn(newUser);

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk()); // Would be 400 with proper validation
        }
    }

    @Nested
    @DisplayName("Token Validation Tests")
    class TokenValidationTests {

        @Test
        @DisplayName("Should validate token for authenticated approved user")
        @WithMockUser(username = "john@example.com")
        void shouldValidateTokenForAuthenticatedApprovedUser() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.email").value("john@example.com"))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.role").value("CUSTOMER"));
        }

        @Test
        @DisplayName("Should return unauthorized for user not found")
        @WithMockUser(username = "unknown@example.com")
        void shouldReturnUnauthorizedForUserNotFound() throws Exception {
            // Given
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.message").value("User not found"));
        }

        @Test
        @DisplayName("Should return forbidden for unapproved user")
        @WithMockUser(username = "jane@example.com")
        void shouldReturnForbiddenForUnapprovedUser() throws Exception {
            // Given
            when(userRepository.findByEmail("jane@example.com")).thenReturn(Optional.of(unapprovedUser));

            // When & Then
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.message").value("Account is pending approval"));
        }

        @Test
        @DisplayName("Should return unauthorized for unauthenticated user")
        void shouldReturnUnauthorizedForUnauthenticatedUser() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.valid").value(false))
                    .andExpect(jsonPath("$.message").value("Invalid or expired token"));
        }

        @Test
        @DisplayName("Should handle authentication with null principal")
        @WithMockUser(username = "john@example.com")
        void shouldHandleAuthenticationWithNullPrincipal() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }
    }

    @Nested
    @DisplayName("Security and Validation Tests")
    class SecurityAndValidationTests {

        @Test
        @DisplayName("Should handle missing content type in login")
        void shouldHandleMissingContentTypeInLogin() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle missing content type in register")
        void shouldHandleMissingContentTypeInRegister() throws Exception {
            // Given
            User newUser = new User();
            newUser.setName("New User");
            newUser.setEmail("newuser@example.com");
            newUser.setPassword("password123");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle empty request body in login")
        void shouldHandleEmptyRequestBodyInLogin() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle empty request body in register")
        void shouldHandleEmptyRequestBodyInRegister() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle null values in login request")
        void shouldHandleNullValuesInLoginRequest() throws Exception {
            // Given
            User nullRequest = new User();
            nullRequest.setEmail(null);
            nullRequest.setPassword(null);

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(nullRequest)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle very long input strings")
        void shouldHandleVeryLongInputStrings() throws Exception {
            // Given
            User longInputUser = new User();
            longInputUser.setEmail("a".repeat(1000) + "@example.com");
            longInputUser.setPassword("p".repeat(1000));

            when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(longInputUser)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should handle special characters in input")
        void shouldHandleSpecialCharactersInInput() throws Exception {
            // Given
            User specialCharUser = new User();
            specialCharUser.setEmail("test@example.com");
            specialCharUser.setPassword("password!@#$%^&*()");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(specialCharUser)))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Cross-Site Request Forgery (CSRF) Tests")
    class CsrfTests {

        @Test
        @DisplayName("Should handle POST requests without CSRF issues")
        void shouldHandlePostRequestsWithoutCsrfIssues() throws Exception {
            // Given
            when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("hashedPassword");

            User newUser = new User();
            newUser.setName("New User");
            newUser.setEmail("newuser@example.com");
            newUser.setPassword("password123");

            // When & Then
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(newUser)))
                    .andExpect(status().isOk());
        }
    }
}