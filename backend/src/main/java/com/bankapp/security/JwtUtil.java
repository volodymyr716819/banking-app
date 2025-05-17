package com.bankapp.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {

    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 10; // 10 hours
    // Fixed secret key string for consistent token validation
    // IMPORTANT: This key should ideally be stored in application properties or environment variables
    // For demo purposes we use a fixed string with Base64 encoding
    private static final String SECRET_KEY_STRING = "YmFua2FwcFNlY3JldEtleUZvckp3dEF1dGhlbnRpY2F0aW9uTXVzdEJlQXRMZWFzdDMyQ2hhcnM=";
    private static final Key SECRET_KEY = Keys.hmacShaKeyFor(java.util.Base64.getDecoder().decode(SECRET_KEY_STRING));

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(SECRET_KEY).build().parseClaimsJws(token).getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            boolean usernameMatches = username.equals(userDetails.getUsername());
            boolean tokenNotExpired = !isTokenExpired(token);
            
            System.out.println("Token validation check:");
            System.out.println("- Username from token: " + username);
            System.out.println("- Username from userDetails: " + userDetails.getUsername());
            System.out.println("- Username matches: " + usernameMatches);
            System.out.println("- Token not expired: " + tokenNotExpired);
            System.out.println("- Token expiration: " + extractExpiration(token));
            System.out.println("- Current time: " + new Date());
            
            return (usernameMatches && tokenNotExpired);
        } catch (Exception e) {
            System.out.println("Error validating token: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 
