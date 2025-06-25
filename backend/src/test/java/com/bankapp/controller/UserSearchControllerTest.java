package com.bankapp.controller;

import com.bankapp.dto.UserSearchResultDTO;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.UserRepository;
import com.bankapp.service.AccountService;
import com.bankapp.service.UserSearchService;
import com.bankapp.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
// only load user controler to test it 
@WebMvcTest(UserController.class)
class UserSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserSearchService userSearchService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private AccountRepository accountRepository;

    @MockBean
    private AccountService accountService;

    @Test
    @WithMockUser(roles = "EMPLOYEE") // Pretend to be logged in as employee
    void searchCustomers_UnapprovedUser_ReturnsEmptyList() throws Exception {
        // Tell the mock service to return empty list
        when(userSearchService.searchUsersByName(eq("Alaa"), any(Authentication.class)))
            .thenReturn(Collections.emptyList());

        // ACT & ASSERT: Make HTTP request and check response GET /api/users/search?name=Alaa returns empty array
        mockMvc.perform(get("/api/users/search")
                .param("name", "Alaa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
// Verify the service was called correctly
        verify(userSearchService).searchUsersByName(eq("Alaa"), any(Authentication.class));
    }

    //Test 2: Approved User with Pending Account
    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void searchCustomers_ApprovedUserPendingAccount_ReturnsUserWithoutIban() throws Exception {
         // ARRANGE: Create user with no IBANs (pending account)
        UserSearchResultDTO trimpakkiros = new UserSearchResultDTO(3L, "Trimpakkiros", 
            Collections.emptyList()); // Empty IBAN list
        
        when(userSearchService.searchUsersByName(eq("Trimpakkiros"), any(Authentication.class)))
            .thenReturn(Arrays.asList(trimpakkiros));

        // Act & Assert: GET /api/users/search?name=Trimpakkiros
        mockMvc.perform(get("/api/users/search")
                .param("name", "Trimpakkiros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Trimpakkiros"))
                .andExpect(jsonPath("$[0].ibans").isArray())
                .andExpect(jsonPath("$[0].ibans").isEmpty());

        verify(userSearchService).searchUsersByName(eq("Trimpakkiros"), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void searchCustomers_ApprovedUserApprovedAccount_ReturnsUserWithIban() throws Exception {
       // ARRANGE: Create user with IBAN
        UserSearchResultDTO panagiotis = new UserSearchResultDTO(4L, "Panagiotis", 
            Arrays.asList("NL91ABNA0417164302"));
        
        when(userSearchService.searchUsersByName(eq("Panagiotis"), any(Authentication.class)))
            .thenReturn(Arrays.asList(panagiotis));

        // Act & Assert: GET /api/users/search?name=Panagiotis
        mockMvc.perform(get("/api/users/search")
                .param("name", "Panagiotis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Panagiotis"))
                .andExpect(jsonPath("$[0].ibans[0]").value("NL91ABNA0417164302"));

        verify(userSearchService).searchUsersByName(eq("Panagiotis"), any(Authentication.class));
    }
}