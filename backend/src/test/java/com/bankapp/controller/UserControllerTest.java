package com.bankapp.controller;

import com.bankapp.model.User;
import com.bankapp.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void approveUser_Success() throws Exception {
        doNothing().when(userService).approveUser(1L);
        
        mockMvc.perform(put("/api/users/1/approve"))
                .andExpect(status().isOk());
        
        verify(userService).approveUser(1L);
    }

    @Test
    void declineUser_Success() throws Exception {
        doNothing().when(userService).declineUser(1L);
        
        mockMvc.perform(put("/api/users/1/decline"))
                .andExpect(status().isOk());
        
        verify(userService).declineUser(1L);
    }
}