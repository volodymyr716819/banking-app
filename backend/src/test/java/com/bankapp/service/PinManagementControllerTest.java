package com.bankapp.controller;

import com.bankapp.dto.PinRequest;
import com.bankapp.service.PinService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PinManagementControllerTest {

    // Mock the PinService dependency used by the controller
    @Mock
    private PinService pinService;

    // Inject mocks into the controller under test
    @InjectMocks
    private PinManagementController controller;

    // Initialize mocks before each test
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testCheckPinStatusTrue() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        // Simulate pinService returning true when checking pin status
        when(pinService.checkPinStatus(1L)).thenReturn(true);

        // Call controller method
        ResponseEntity<?> response = controller.checkPinStatus(1L, auth);

        // Assert the response contains the expected boolean value
        assertEquals(true, ((java.util.Map<?, ?>) response.getBody()).get("pinCreated"));
    }

    @Test
    void testCreatePinSuccess() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        // Create a sample PinRequest
        PinRequest req = new PinRequest();
        req.setAccountId(1L);
        req.setPin("1234".toCharArray());

        // Call controller method
        controller.createPin(req, auth);

        // Verify pinService.createPin was called with correct input
        verify(pinService).createPin(req);
    }

    @Test
    void testVerifyPinValid() {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        // Prepare test input
        PinRequest req = new PinRequest();
        req.setAccountId(1L);
        req.setPin("1234".toCharArray());

        // Simulate a successful pin verification
        when(pinService.verifyPin(req)).thenReturn(true);

        // Call controller method
        ResponseEntity<?> response = controller.verifyPin(req, auth);

        // Assert response indicates the pin is valid
        assertEquals(true, ((java.util.Map<?, ?>) response.getBody()).get("valid"));
    }

    @Test
    void testChangePinSuccess() {
         Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@example.com");

        // Setup a PinRequest with old and new pin
        PinRequest req = new PinRequest();
        req.setAccountId(1L);
        req.setPin("1234".toCharArray());
        req.setNewPin("5678".toCharArray());

        // Call controller method
        controller.changePin(req, auth);

        // Verify that pinService.changePin was called correctly
        verify(pinService).changePin(req);
    }
}
