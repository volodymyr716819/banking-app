package com.bankapp.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.bankapp.model.Account;
import com.bankapp.model.User;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;

@WebMvcTest(UserController.class)
@DisplayName("UserController Integration Tests")
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccountRepository accountRepository;

    private User testUser;
    private User employeeUser;
    private User pendingUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Setup test user
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setRole("CUSTOMER");
        testUser.setApproved(true);

        // Setup employee user
        employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setName("Jane Employee");
        employeeUser.setEmail("jane@example.com");
        employeeUser.setRole("EMPLOYEE");
        employeeUser.setApproved(true);

        // Setup pending user
        pendingUser = new User();
        pendingUser.setId(3L);
        pendingUser.setName("Pending User");
        pendingUser.setEmail("pending@example.com");
        pendingUser.setRole("CUSTOMER");
        pendingUser.setApproved(false);

        // Setup test account
        testAccount = new Account();
        testAccount.setId(1L);
        testAccount.setUser(testUser);
        testAccount.setType("CHECKING");
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setApproved(true);
        testAccount.setClosed(false);
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should get all users")
        @WithMockUser
        void shouldGetAllUsers() throws Exception {
            // Given
            when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, employeeUser));

            // When & Then
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2));

            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should return empty list when no users exist")
        @WithMockUser
        void shouldReturnEmptyListWhenNoUsersExist() throws Exception {
            // Given
            when(userRepository.findAll()).thenReturn(Arrays.asList());

            // When & Then
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should get user by valid ID")
        @WithMockUser
        void shouldGetUserByValidId() throws Exception {
            // Given
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("John Doe"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));

            verify(userRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return not found for invalid ID")
        @WithMockUser
        void shouldReturnNotFoundForInvalidId() throws Exception {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/users/999"))
                    .andExpect(status().isNotFound());

            verify(userRepository).findById(999L);
        }

        @Test
        @DisplayName("Should handle invalid path parameter")
        @WithMockUser
        void shouldHandleInvalidPathParameter() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/invalid"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should successfully delete existing user")
        @WithMockUser
        void shouldSuccessfullyDeleteExistingUser() throws Exception {
            // Given
            when(userRepository.existsById(1L)).thenReturn(true);

            // When & Then
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User deleted."));

            verify(userRepository).existsById(1L);
            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("Should return not found when deleting non-existent user")
        @WithMockUser
        void shouldReturnNotFoundWhenDeletingNonExistentUser() throws Exception {
            // Given
            when(userRepository.existsById(999L)).thenReturn(false);

            // When & Then
            mockMvc.perform(delete("/api/users/999"))
                    .andExpect(status().isNotFound());

            verify(userRepository).existsById(999L);
            verify(userRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Pending Users Tests")
    class PendingUsersTests {

        @Test
        @DisplayName("Should get pending users for employee")
        @WithMockUser(authorities = {"ROLE_EMPLOYEE"})
        void shouldGetPendingUsersForEmployee() throws Exception {
            // Given
            when(userRepository.findByApprovedFalse()).thenReturn(Arrays.asList(pendingUser));

            // When & Then
            mockMvc.perform(get("/api/users/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(3));

            verify(userRepository).findByApprovedFalse();
        }

        @Test
        @DisplayName("Should return forbidden for non-employee")
        @WithMockUser(authorities = {"ROLE_CUSTOMER"})
        void shouldReturnForbiddenForNonEmployee() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/pending"))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).findByApprovedFalse();
        }

        @Test
        @DisplayName("Should require authentication")
        void shouldRequireAuthentication() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/pending"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("Approve User Tests")
    class ApproveUserTests {

        @Test
        @DisplayName("Should successfully approve user as employee")
        @WithMockUser(authorities = {"ROLE_EMPLOYEE"})
        void shouldSuccessfullyApproveUserAsEmployee() throws Exception {
            // Given
            when(userRepository.findById(3L)).thenReturn(Optional.of(pendingUser));
            when(userRepository.save(any(User.class))).thenReturn(pendingUser);

            // When & Then
            mockMvc.perform(post("/api/users/3/approve"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("User approved successfully."));

            verify(userRepository).findById(3L);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should return not found when user does not exist")
        @WithMockUser(authorities = {"ROLE_EMPLOYEE"})
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
            // Given
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(post("/api/users/999/approve"))
                    .andExpect(status().isNotFound());

            verify(userRepository).findById(999L);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should return forbidden for non-employee")
        @WithMockUser(authorities = {"ROLE_CUSTOMER"})
        void shouldReturnForbiddenForNonEmployee() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/users/3/approve"))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("Approved Customers Tests")
    class ApprovedCustomersTests {

        @Test
        @DisplayName("Should get approved customers for employee")
        @WithMockUser(authorities = {"ROLE_EMPLOYEE"})
        void shouldGetApprovedCustomersForEmployee() throws Exception {
            // Given
            when(userRepository.findByApprovedTrue()).thenReturn(Arrays.asList(testUser, employeeUser));

            // When & Then
            mockMvc.perform(get("/api/users/approved"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1)); // Only customer, not employee

            verify(userRepository).findByApprovedTrue();
        }

        @Test
        @DisplayName("Should return forbidden for non-employee")
        @WithMockUser(authorities = {"ROLE_CUSTOMER"})
        void shouldReturnForbiddenForNonEmployee() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/approved"))
                    .andExpect(status().isForbidden());

            verify(userRepository, never()).findByApprovedTrue();
        }
    }

    @Nested
    @DisplayName("Search Users Tests")
    class SearchUsersTests {

        @Test
        @DisplayName("Should search users by term when authenticated")
        @WithMockUser(username = "john@example.com")
        void shouldSearchUsersByTermWhenAuthenticated() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findCustomersBySearchTerm("John")).thenReturn(Arrays.asList(testUser));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(userRepository).findCustomersBySearchTerm("John");
        }

        @Test
        @DisplayName("Should search users by IBAN format")
        @WithMockUser(username = "john@example.com")
        void shouldSearchUsersByIbanFormat() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findCustomersBySearchTerm("NLBANK1")).thenReturn(Arrays.asList());
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", "NLBANK1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(accountRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return bad request when user not authenticated")
        void shouldReturnBadRequestWhenUserNotAuthenticated() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", "John"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("User not authenticated"));
        }

        @Test
        @DisplayName("Should return bad request when user not found")
        @WithMockUser(username = "unknown@example.com")
        void shouldReturnBadRequestWhenUserNotFound() throws Exception {
            // Given
            when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", "John"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("User not found"));
        }

        @Test
        @DisplayName("Should return bad request when user not approved")
        @WithMockUser(username = "pending@example.com")
        void shouldReturnBadRequestWhenUserNotApproved() throws Exception {
            // Given
            when(userRepository.findByEmail("pending@example.com")).thenReturn(Optional.of(pendingUser));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", "John"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("User not approved"));
        }

        @Test
        @DisplayName("Should search by legacy name parameter")
        @WithMockUser(username = "john@example.com")
        void shouldSearchByLegacyNameParameter() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findByNameAndEmailAndRole("John", null, "customer"))
                .thenReturn(Arrays.asList(testUser));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("name", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(userRepository).findByNameAndEmailAndRole("John", null, "customer");
        }

        @Test
        @DisplayName("Should search by legacy email parameter")
        @WithMockUser(username = "john@example.com")
        void shouldSearchByLegacyEmailParameter() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findByNameAndEmailAndRole(null, "john@example.com", "customer"))
                .thenReturn(Arrays.asList(testUser));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("email", "john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(userRepository).findByNameAndEmailAndRole(null, "john@example.com", "customer");
        }

        @Test
        @DisplayName("Should search by legacy IBAN parameter")
        @WithMockUser(username = "john@example.com")
        void shouldSearchByLegacyIbanParameter() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("iban", "NLBANK1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1));

            verify(accountRepository).findById(1L);
        }

        @Test
        @DisplayName("Should return bad request when no search parameters provided")
        @WithMockUser(username = "john@example.com")
        void shouldReturnBadRequestWhenNoSearchParametersProvided() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/users/search"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("At least one search parameter is required"));
        }

        @Test
        @DisplayName("Should handle invalid IBAN format gracefully")
        @WithMockUser(username = "john@example.com")
        void shouldHandleInvalidIbanFormatGracefully() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("iban", "INVALID_IBAN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("Legacy Search Endpoints Tests")
    class LegacySearchEndpointsTests {

        @Test
        @DisplayName("Should handle find by name endpoint")
        @WithMockUser(username = "john@example.com")
        void shouldHandleFindByNameEndpoint() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findByNameAndEmailAndRole("John", null, "customer"))
                .thenReturn(Arrays.asList(testUser));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/find-by-name")
                    .param("name", "John"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }

        @Test
        @DisplayName("Should handle find by email endpoint")
        @WithMockUser(username = "john@example.com")
        void shouldHandleFindByEmailEndpoint() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));
            when(userRepository.findByNameAndEmailAndRole(null, "john@example.com", "customer"))
                .thenReturn(Arrays.asList(testUser));
            when(accountRepository.findByUserId(1L)).thenReturn(Arrays.asList(testAccount));

            // When & Then
            mockMvc.perform(get("/api/users/find-by-email")
                    .param("email", "john@example.com"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }

    @Nested
    @DisplayName("Security and Validation Tests")
    class SecurityAndValidationTests {

        @Test
        @DisplayName("Should require authentication for search endpoints")
        void shouldRequireAuthenticationForSearchEndpoints() throws Exception {
            mockMvc.perform(get("/api/users/search")
                    .param("term", "John"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get("/api/users/find-by-name")
                    .param("name", "John"))
                    .andExpect(status().isBadRequest());

            mockMvc.perform(get("/api/users/find-by-email")
                    .param("email", "john@example.com"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should handle empty search parameters")
        @WithMockUser(username = "john@example.com")
        void shouldHandleEmptySearchParameters() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("term", ""))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("At least one search parameter is required"));
        }

        @Test
        @DisplayName("Should handle whitespace-only search parameters")
        @WithMockUser(username = "john@example.com")
        void shouldHandleWhitespaceOnlySearchParameters() throws Exception {
            // Given
            when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(testUser));

            // When & Then
            mockMvc.perform(get("/api/users/search")
                    .param("name", "   "))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("At least one search parameter is required"));
        }
    }
}