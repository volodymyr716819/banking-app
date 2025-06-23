package com.bankapp.controller;

import com.bankapp.dto.UserDTO;
import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.UserSearchService;
import com.bankapp.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock private UserRepository userRepository;
    @Mock private UserSearchService userSearchService;
    @Mock private UserService userService;
    @Mock private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_shouldReturnUserDTOList() {
        // Arrange - mock repository response
        User user = new User();
        user.setId(1L);
        user.setName("John");
        user.setEmail("john@example.com");
        user.setRole("CUSTOMER");
        user.setApproved(true);

        when(userRepository.findAll()).thenReturn(List.of(user));

        // Act
        List<UserDTO> result = userController.getAllUsers();

        // Assert
        assertEquals(1, result.size());
        assertEquals("John", result.get(0).getName());
    }

    @Test
    void getUserById_shouldReturnUserIfExists() {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setName("Alice");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        ResponseEntity<UserDTO> response = userController.getUserById(1L);

        // Assert
        assertTrue(response.getStatusCode().is2xxSuccessful());
        assertEquals("Alice", response.getBody().getName());
    }

    @Test
    void getUserById_shouldReturn404IfNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        ResponseEntity<UserDTO> response = userController.getUserById(2L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void deleteUser_shouldReturnOkIfDeleted() {
        when(userRepository.existsById(1L)).thenReturn(true);

        ResponseEntity<?> response = userController.deleteUser(1L);

        assertEquals(200, response.getStatusCodeValue());
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_shouldReturnNotFoundIfMissing() {
        when(userRepository.existsById(1L)).thenReturn(false);

        ResponseEntity<?> response = userController.deleteUser(1L);

        assertEquals(404, response.getStatusCodeValue());
    }

    @Test
    void approveUser_shouldCallServiceAndReturnSuccess() {
        // Test controller delegation to userService.approveUser
        ResponseEntity<?> response = userController.approveUser(1L);

        assertEquals(200, response.getStatusCodeValue());
        verify(userService).approveUser(1L);
    }

    @Test
    void approveUser_shouldReturnBadRequestIfFails() {
        // Simulate exception from service layer
        doThrow(new RuntimeException("error")).when(userService).approveUser(1L);

        ResponseEntity<?> response = userController.approveUser(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void declineUser_shouldCallServiceAndReturnSuccess() {
        ResponseEntity<?> response = userController.declineUser(1L);

        assertEquals(200, response.getStatusCodeValue());
        verify(userService).declineUser(1L);
    }

    @Test
    void declineUser_shouldReturnBadRequestIfFails() {
        doThrow(new RuntimeException("error")).when(userService).declineUser(1L);

        ResponseEntity<?> response = userController.declineUser(1L);

        assertEquals(400, response.getStatusCodeValue());
    }

    @Test
    void searchUsers_shouldReturnOkWithResults() {
        // Test generic search with mocked result
        when(userSearchService.searchUsers("john", null, null, null, authentication))
                .thenReturn(List.of());

        ResponseEntity<?> response = userController.searchUsers("john", null, null, null, authentication);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void searchUsers_shouldReturnBadRequestOnException() {
        // Simulate validation error thrown from search service
        when(userSearchService.searchUsers(null, null, null, null, authentication))
                .thenThrow(new IllegalArgumentException("Missing params"));

        ResponseEntity<?> response = userController.searchUsers(null, null, null, null, authentication);

        assertEquals(400, response.getStatusCodeValue());
    }
}
