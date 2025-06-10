package com.bankapp.controller;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.service.UserSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the customer search API endpoint in UserController.
 * Tests both success and error scenarios for the search functionality.
 */
class UserSearchControllerTest {

    @Mock
    private UserSearchService userSearchService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void searchUsers_withTerm_returnsResults() {
        // Arrange - mock the service response
        List<String> ibans = new ArrayList<>();
        ibans.add("NL00BANK0000000001");
        UserSearchResultDTO resultDTO = new UserSearchResultDTO(1L, "Alaa Aldrobe", ibans);
        
        when(userSearchService.searchUsers("Alaa", null, null, null, authentication))
            .thenReturn(List.of(resultDTO));

        // Act
        ResponseEntity<?> response = userController.searchUsers("Alaa", null, null, null, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<UserSearchResultDTO> results = (List<UserSearchResultDTO>) response.getBody();
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Alaa Aldrobe", results.get(0).getName());
        assertEquals(1L, results.get(0).getId());
        assertEquals(1, results.get(0).getIbans().size());
        assertEquals("NL00BANK0000000001", results.get(0).getIbans().get(0));
    }

    @Test
    void searchUsers_withNameAndEmail_returnsResults() {
        // Arrange - mock service response for name and email search
        List<String> ibans = new ArrayList<>();
        ibans.add("NL00BANK0000000001");
        UserSearchResultDTO resultDTO = new UserSearchResultDTO(1L, "Alaa Aldrobe", ibans);
        
        when(userSearchService.searchUsers(null, "Alaa", "alaa@example.com", null, authentication))
            .thenReturn(List.of(resultDTO));

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            null, "Alaa", "alaa@example.com", null, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<UserSearchResultDTO> results = (List<UserSearchResultDTO>) response.getBody();
        
        assertNotNull(results);
        assertEquals(1, results.size());
    }

    @Test
    void searchUsers_withIban_returnsResults() {
        // Arrange - mock service response for IBAN search
        String iban = "NL00BANK0000000001";
        List<String> ibans = new ArrayList<>();
        ibans.add(iban);
        UserSearchResultDTO resultDTO = new UserSearchResultDTO(1L, "Alaa Aldrobe", ibans);
        
        when(userSearchService.searchUsers(null, null, null, iban, authentication))
            .thenReturn(List.of(resultDTO));

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            null, null, null, iban, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<UserSearchResultDTO> results = (List<UserSearchResultDTO>) response.getBody();
        
        assertNotNull(results);
        assertEquals(1, results.size());
        assertTrue(results.get(0).getIbans().contains(iban));
    }

    @Test
    void searchUsers_withMultipleResults_returnsAllResults() {
        // Arrange - mock service returning multiple customers
        UserSearchResultDTO result1 = new UserSearchResultDTO(
            1L, "Alaa Aldrobe", List.of("NL00BANK0000000001"));
        UserSearchResultDTO result2 = new UserSearchResultDTO(
            2L, "Volodymyr Gulchenko", List.of("NL00BANK0000000002"));
        
        when(userSearchService.searchUsers("a", null, null, null, authentication))
            .thenReturn(Arrays.asList(result1, result2));

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            "a", null, null, null, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<UserSearchResultDTO> results = (List<UserSearchResultDTO>) response.getBody();
        
        assertNotNull(results);
        assertEquals(2, results.size());
    }

    @Test
    void searchUsers_emptyResults_returnsEmptyList() {
        // Arrange - mock service returning empty results
        when(userSearchService.searchUsers("nonexistent", null, null, null, authentication))
            .thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            "nonexistent", null, null, null, authentication);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        @SuppressWarnings("unchecked")
        List<UserSearchResultDTO> results = (List<UserSearchResultDTO>) response.getBody();
        
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void searchUsers_serviceThrowsException_returnsBadRequest() {
        // Arrange - mock service throwing exception
        when(userSearchService.searchUsers(null, null, null, null, authentication))
            .thenThrow(new IllegalArgumentException("At least one search parameter is required"));

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            null, null, null, null, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("parameter is required"));
    }

    @Test
    void searchUsers_serviceThrowsAuthenticationException_returnsBadRequest() {
        // Arrange - mock service throwing authentication exception
        when(userSearchService.searchUsers("test", null, null, null, authentication))
            .thenThrow(new IllegalArgumentException("User not authenticated"));

        // Act
        ResponseEntity<?> response = userController.searchUsers(
            "test", null, null, null, authentication);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("not authenticated"));
    }

    @Test
    void searchUsers_legacyEndpoints_redirectToMainEndpoint() {
        // Test legacy endpoints redirect to main search endpoint
        
        // Arrange - mock service for name search
        UserSearchResultDTO resultDTO = new UserSearchResultDTO(
            1L, "Alaa Aldrobe", List.of("NL00BANK0000000001"));
        when(userSearchService.searchUsers("Alaa", null, null, null, authentication))
            .thenReturn(List.of(resultDTO));
        
        // Act & Assert - find-by-name redirects to search
        ResponseEntity<?> nameResponse = userController.searchUsersByName("Alaa", authentication);
        assertEquals(HttpStatus.OK, nameResponse.getStatusCode());
        
        // Arrange - mock service for email search
        when(userSearchService.searchUsers(null, null, "alaa@example.com", null, authentication))
            .thenReturn(List.of(resultDTO));
        
        // Act & Assert - find-by-email redirects to search
        ResponseEntity<?> emailResponse = userController.searchUsersByEmail("alaa@example.com", authentication);
        assertEquals(HttpStatus.OK, emailResponse.getStatusCode());
    }
}