package com.bankapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class ApplicationContextTest {

    @Test
    public void contextLoads() {
        // This test will pass if the Spring context loads successfully
    }
}