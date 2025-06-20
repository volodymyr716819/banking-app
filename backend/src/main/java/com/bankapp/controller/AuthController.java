package com.bankapp.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.AuthenticationException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import com.bankapp.model.User;
import com.bankapp.model.enums.RegistrationStatus;
import com.bankapp.service.UserService;
import com.bankapp.security.JwtUtil;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Endpoints for user login, registration, and token validation")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Handles login requests by authenticating credentials and returning JWT
    @Operation(summary = "Authenticate user and return JWT token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful, JWT returned"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User request) {
        try {
            // 1) authenticate credentials
            Authentication auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

            // 2) load the User (now credentials-only)
            User user = userService.validateLogin(request)
                    .orElseThrow(() -> new BadCredentialsException("Invalid"));

            // 3) generate the JWT
            UserDetails ud = (UserDetails) auth.getPrincipal();
            String jwt = jwtUtil.generateToken(ud);

            // 4) build response map, including status
            Map<String, Object> body = new HashMap<>();
            body.put("token", jwt);
            body.put("id", user.getId());
            body.put("email", user.getEmail());
            body.put("name", user.getName());
            body.put("role", user.getRole());
            body.put("registrationStatus", user.getRegistrationStatus().name()); // ← key line

            return ResponseEntity.ok(body);

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Invalid email or password."));
        }
    }

    // Registers a new user if email is not already taken
    @Operation(summary = "Register a new user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful, pending approval"),
            @ApiResponse(responseCode = "400", description = "Validation error or email already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            userService.registerUser(user);
            return ResponseEntity.ok(Map.of("message", "Registration successful. Your account is pending approval."));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
        }
    }

    // Validates the token and user status for authenticated sessions
    @Operation(summary = "Validate JWT token and return user info")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid and user is authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "403", description = "Access forbidden")
    })
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(Authentication authentication) {
        try {
            Optional<User> userOpt = userService.validateAuthentication(authentication);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("valid", false, "message", "Invalid or expired token"));
            }

            User user = userOpt.get();
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "id", user.getId(),
                    "email", user.getEmail(),
                    "name", user.getName(),
                    "role", user.getRole()));
        } catch (RuntimeException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("valid", false, "message", ex.getMessage()));
        }
    }
}
