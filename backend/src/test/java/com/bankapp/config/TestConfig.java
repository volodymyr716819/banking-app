package com.bankapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import com.bankapp.util.PinHashUtil;

@TestConfiguration
@TestPropertySource(locations = "classpath:application-test.properties")
public class TestConfig {

    @Bean
    @Primary
    public PasswordEncoder testPasswordEncoder() {
        // Use a weaker configuration for faster tests
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    @Primary
    public PinHashUtil testPinHashUtil() {
        return new PinHashUtil();
    }
}