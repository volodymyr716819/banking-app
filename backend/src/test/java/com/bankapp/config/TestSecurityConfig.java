package com.bankapp.config;

import com.bankapp.security.JwtUtil;
import com.bankapp.security.TestJwtUtil;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Test security configuration that overrides the application's security settings for testing purposes
 */
@TestConfiguration
@EnableMethodSecurity
public class TestSecurityConfig {

    @Bean
    @Primary
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        // Simplified security configuration for tests - permit all requests
        http
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll());
        
        return http.build();
    }
    
    // This method is not needed anymore as the primary bean will be used
    /* 
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return testSecurityFilterChain(http);
    }
    */
    
    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // Use BCryptPasswordEncoder with a prefix to avoid the "null" encoder issue
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    @Primary
    public JwtUtil jwtUtil() {
        return new TestJwtUtil();
    }
}