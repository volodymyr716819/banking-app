package com.bankapp.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Test implementation of JwtUtil for use in tests that works with actual user details
 */
@Component
public class TestJwtUtil extends JwtUtil {
    
    private final String SECRET_KEY = "testSecretKeyForJWTTokenThatIsLongEnoughForHS256Algorithm";
    private final long JWT_TOKEN_VALIDITY = 24 * 60 * 60 * 1000; // 24 hours
    
    private Map<String, String> tokenUserMap = new HashMap<>();
    
    @Override
    public String extractUsername(String token) {
        // For our simplified test approach, just return the username from the map
        if (tokenUserMap.containsKey(token)) {
            return tokenUserMap.get(token);
        }
        
        // For tokens that follow the test-token-username pattern
        if (token.startsWith("test-token-")) {
            if (token.equals("test-token-customer")) {
                return "customer@test.com";
            } else if (token.equals("test-token-employee")) {
                return "employee@test.com";
            }
            return token.substring("test-token-".length());
        }
        
        return "unknown-user";
    }
    
    @Override
    public Date extractExpiration(String token) {
        // Always return a future date for testing
        return new Date(System.currentTimeMillis() + 10000);
    }
    
    @Override
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        // This is a simplified implementation for testing
        return null;
    }
    
    @Override
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        // Create a simple token with username as payload for testing
        String token = "test-token-" + userDetails.getUsername();
        
        // Store the mapping for validation
        tokenUserMap.put(token, userDetails.getUsername());
        return token;
    }
    
    @Override
    public Boolean validateToken(String token, UserDetails userDetails) {
        // For testing, bypass authentication but return true only for our special test tokens
        return token != null && (token.startsWith("test-token-") || tokenUserMap.containsKey(token));
    }
}