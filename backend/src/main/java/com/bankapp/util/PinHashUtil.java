package com.bankapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for securely hashing and verifying PINs
 */
@Component
public class PinHashUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PinHashUtil.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Hash a PIN using BCrypt algorithm
     * 
     * @param pin Plain text PIN to hash
     * @return Hashed PIN
     */
    public String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            logger.warn("Attempt to hash null or empty PIN");
            return "";
        }
        return encoder.encode(pin);
    }
    
    /**
     * Verify if a plaintext PIN matches a hashed PIN
     * 
     * @param plainPin Plain text PIN to verify
     * @param hashedPin Hashed PIN to compare against
     * @return true if the PIN matches, false otherwise
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        if (plainPin == null || plainPin.isEmpty() || hashedPin == null || hashedPin.isEmpty()) {
            logger.warn("Attempt to verify null or empty PIN");
            return false;
        }
        return encoder.matches(plainPin, hashedPin);
    }
}